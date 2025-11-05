/* shannon_revealer.c
 * Read diagnostic messages from Samsung Shannon modem devices (/dev/umts_dm0).
 * Messages are output using a Linux FIFO pipe in DiagRevealer format.
 *
 * Based on: diag_revealer.c by Jiayao Li, Yuanjie Li, Haotian Deng
 * Protocol: Samsung Shannon Diagnostic Monitor (SDM)
 */

/* This program writes to FIFO using the DiagRevealer packet format:
 *    type: 2-byte integer. Can be one of the following values:
 *      1: LOG
 *      2: START_LOG_FILE, indicating the creation of a new log file.
 *      3: END_LOG_FILE, indicating the end of a log file.
 *    length: 2-byte integer. The total number of bytes in this packet
 *      (excluding the type field).
 * If "type" is LOG, there are two other fields:
 *    timestamp: 8-byte double float number. A POSIX timestamp representing
 *      when this log is received from the device.
 *    payload: byte stream of variable length (Shannon frame with delimiters).
 */

#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <sys/time.h>
#include <endian.h>

#include <android/log.h>
#define LOG_TAG    "shannon_revealer"

#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Shannon protocol constants
#define SHANNON_START_MARKER 0x7F
#define SHANNON_END_MARKER 0x7E
#define SHANNON_HEADER_SIZE 14
#define SHANNON_MIN_FRAME_SIZE 16  // start + 14-byte header + end

// Buffer sizes
#define BUFFER_SIZE 8192
#define MAX_FRAME_SIZE 4096

// FIFO message types
#define FIFO_MSG_TYPE_LOG 1
#define FIFO_MSG_TYPE_START_LOG_FILE 2
#define FIFO_MSG_TYPE_END_LOG_FILE 3

// Shannon revealer version
#define SHANNON_REVEALER_VERSION "1.1"

// Default device path
#define DEFAULT_DEVICE_PATH "/dev/umts_dm0"

// Shannon SDM protocol constants for initialization
#define SDM_DIRECTION_DM 0xA0

// SDM control command definitions
#define CONTROL_START 0x00
#define CHANGE_UPDATE_PERIOD_REQUEST 0x06
#define COMMON_ITEM_SELECT_REQUEST 0x10
#define LTE_ITEM_SELECT_REQUEST 0x20
#define EDGE_ITEM_SELECT_REQUEST 0x30
#define HSPA_ITEM_SELECT_REQUEST 0x40
#define CDMA_ITEM_SELECT_REQUEST 0x44
#define COMMON_ITEM_REFRESH_REQUEST 0x12
#define LTE_ITEM_REFRESH_REQUEST 0x22
#define EDGE_ITEM_REFRESH_REQUEST 0x32
#define HSPA_ITEM_REFRESH_REQUEST 0x42
#define CDMA_ITEM_REFRESH_REQUEST 0x46
#define TRACE_STOP_REQUEST 0x52
#define ILM_STOP_REQUEST 0x72
#define TRIGGER_TABLE_REQUEST 0xC0
#define TCPIP_DUMP_REQUEST 0x90

#define MAX_SDM_PACKET_SIZE 256

// Read buffer for frame extraction
typedef struct {
    uint8_t data[BUFFER_SIZE];
    size_t len;     // Bytes currently in buffer
    size_t pos;     // Current read position
} ReadBuffer;

// Global file descriptors for signal handler
static int g_device_fd = -1;
static int g_fifo_fd = -1;
static volatile sig_atomic_t g_shutdown = 0;

// Statistics for initialization
static unsigned long g_init_success_count = 0;
static unsigned long g_init_failure_count = 0;
static double g_last_init_time_ms = 0.0;

/*
 * Signal handler for graceful shutdown
 */
static void
signal_handler(int signo)
{
    if (signo == SIGINT || signo == SIGTERM) {
        LOGI("Received signal %d, shutting down gracefully", signo);
        g_shutdown = 1;
    } else if (signo == SIGPIPE) {
        LOGW("Received SIGPIPE (FIFO reader disconnected)");
        // Don't shutdown, just log it
    }
}

/*
 * Get current POSIX timestamp as double
 */
static double
get_posix_timestamp(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (double)(tv.tv_sec) + (double)(tv.tv_usec) / 1.0e6;
}

/*
 * Generate SDM packet according to Shannon protocol
 *
 * Packet structure:
 *   0x7F                         - Start marker
 *   length1 (uint16_le)          - Total packet length + 3
 *   zero (uint8)                 - Always 0
 *   length2 (uint16_le)          - Payload length + header (length1)
 *   stamp (uint16_le)            - Always 0
 *   direction (uint8)            - 0xA0 for IPC_DM_CMD
 *   group (uint8)                - Command group (0x00 for control)
 *   command (uint8)              - Command ID
 *   timestamp (uint32_le)        - Timestamp (usually 0)
 *   payload (variable)           - Command payload
 *   0x7E                         - End marker
 *
 * Returns: packet length on success, -1 on error
 */
static int
generate_sdm_packet(uint8_t *out_buf, size_t out_buf_size,
                   uint8_t direction, uint8_t group, uint8_t command,
                   const uint8_t *payload, size_t payload_len,
                   uint32_t timestamp)
{
    // Calculate packet length: 2 + 3 + 4 + payload + 2
    // = 14 bytes header + payload + start/end markers
    uint16_t pkt_len = 2 + 3 + 4 + payload_len + 2;
    uint16_t length1 = pkt_len + 3;  // Total length + 3
    uint16_t length2 = pkt_len;      // Inner length

    // Check output buffer size
    size_t total_size = 1 + length1 + 1;  // start + header+payload + end
    if (total_size > out_buf_size) {
        LOGE("SDM packet buffer too small: need %zu, have %zu", total_size, out_buf_size);
        return -1;
    }

    uint8_t *p = out_buf;

    // Start marker
    *p++ = SHANNON_START_MARKER;

    // length1 (little-endian uint16)
    *p++ = (length1 & 0xFF);
    *p++ = (length1 >> 8) & 0xFF;

    // zero byte
    *p++ = 0x00;

    // length2 (little-endian uint16)
    *p++ = (length2 & 0xFF);
    *p++ = (length2 >> 8) & 0xFF;

    // stamp (little-endian uint16)
    *p++ = 0x00;
    *p++ = 0x00;

    // direction
    *p++ = direction;

    // group
    *p++ = group;

    // command
    *p++ = command;

    // timestamp (little-endian uint32)
    *p++ = (timestamp & 0xFF);
    *p++ = (timestamp >> 8) & 0xFF;
    *p++ = (timestamp >> 16) & 0xFF;
    *p++ = (timestamp >> 24) & 0xFF;

    // payload
    if (payload_len > 0 && payload != NULL) {
        memcpy(p, payload, payload_len);
        p += payload_len;
    }

    // End marker
    *p++ = SHANNON_END_MARKER;

    return (int)(p - out_buf);
}

/*
 * Send a Shannon SDM control command
 *
 * @param fd File descriptor for /dev/umts_dm0
 * @param group Command group (typically 0x00 for control)
 * @param command Command ID
 * @param payload Command payload (can be NULL)
 * @param payload_len Payload length
 * @return 0 on success, -1 on error
 */
static int
send_control_command(int fd, uint8_t group, uint8_t command,
                    const uint8_t *payload, size_t payload_len)
{
    uint8_t pkt[MAX_SDM_PACKET_SIZE];
    int pkt_len = generate_sdm_packet(pkt, sizeof(pkt), SDM_DIRECTION_DM, group, command,
                                     payload, payload_len, 0);

    if (pkt_len < 0) {
        LOGE("Failed to generate SDM packet for command 0x%02x", command);
        return -1;
    }

    LOGD("Sending SDM command: group=0x%02x, cmd=0x%02x, len=%d", group, command, pkt_len);

    ssize_t ret = write(fd, pkt, pkt_len);
    if (ret != pkt_len) {
        LOGE("Write failed for command 0x%02x: expected %d, got %zd: %s",
             command, pkt_len, ret, strerror(errno));
        return -1;
    }

    return 0;
}

/*
 * Initialize Shannon modem diagnostic mode
 * Sends the 9-step initialization sequence
 *
 * @param fd File descriptor for /dev/umts_dm0 (must be O_RDWR)
 * @return 0 on success, -1 on error
 */
static int
initialize_shannon_modem(int fd)
{
    struct timeval start_time, end_time;
    gettimeofday(&start_time, NULL);

    LOGI("=== Starting Shannon modem initialization ===");

    // Step 1: CONTROL_START with magic 0x41414141
    LOGI("Init step 1/9: Sending CONTROL_START");
    uint8_t start_payload[] = {0x41, 0x41, 0x41, 0x41};  // Magic value
    if (send_control_command(fd, 0x00, CONTROL_START, start_payload, sizeof(start_payload)) < 0) {
        LOGE("Init step 1/9: FAILED");
        return -1;
    }
    LOGI("Init step 1/9: OK");
    usleep(100000);  // 100ms delay

    // Step 2: CHANGE_UPDATE_PERIOD_REQUEST with period=5
    LOGI("Init step 2/9: Setting update period to 5");
    uint8_t period_payload[] = {0x05};
    if (send_control_command(fd, 0x00, CHANGE_UPDATE_PERIOD_REQUEST,
                            period_payload, sizeof(period_payload)) < 0) {
        LOGE("Init step 2/9: FAILED");
        return -1;
    }
    LOGI("Init step 2/9: OK");
    usleep(100000);

    // Step 3: Deselect all items (0x00 = deselect all)
    LOGI("Init step 3/9: Deselecting all items");
    uint8_t deselect_payload[] = {0x00};

    if (send_control_command(fd, 0x00, COMMON_ITEM_SELECT_REQUEST,
                            deselect_payload, sizeof(deselect_payload)) < 0) {
        LOGE("Init step 3/9: FAILED at COMMON_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, LTE_ITEM_SELECT_REQUEST,
                            deselect_payload, sizeof(deselect_payload)) < 0) {
        LOGE("Init step 3/9: FAILED at LTE_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, EDGE_ITEM_SELECT_REQUEST,
                            deselect_payload, sizeof(deselect_payload)) < 0) {
        LOGE("Init step 3/9: FAILED at EDGE_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, HSPA_ITEM_SELECT_REQUEST,
                            deselect_payload, sizeof(deselect_payload)) < 0) {
        LOGE("Init step 3/9: FAILED at HSPA_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, CDMA_ITEM_SELECT_REQUEST,
                            deselect_payload, sizeof(deselect_payload)) < 0) {
        LOGE("Init step 3/9: FAILED at CDMA_ITEM_SELECT");
        return -1;
    }
    LOGI("Init step 3/9: OK");
    usleep(100000);

    // Step 4: Enable all items (0xFF = select all)
    LOGI("Init step 4/9: Enabling all items");
    uint8_t select_all_payload[] = {0xFF};

    if (send_control_command(fd, 0x00, COMMON_ITEM_SELECT_REQUEST,
                            select_all_payload, sizeof(select_all_payload)) < 0) {
        LOGE("Init step 4/9: FAILED at COMMON_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, LTE_ITEM_SELECT_REQUEST,
                            select_all_payload, sizeof(select_all_payload)) < 0) {
        LOGE("Init step 4/9: FAILED at LTE_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, EDGE_ITEM_SELECT_REQUEST,
                            select_all_payload, sizeof(select_all_payload)) < 0) {
        LOGE("Init step 4/9: FAILED at EDGE_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, HSPA_ITEM_SELECT_REQUEST,
                            select_all_payload, sizeof(select_all_payload)) < 0) {
        LOGE("Init step 4/9: FAILED at HSPA_ITEM_SELECT");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, CDMA_ITEM_SELECT_REQUEST,
                            select_all_payload, sizeof(select_all_payload)) < 0) {
        LOGE("Init step 4/9: FAILED at CDMA_ITEM_SELECT");
        return -1;
    }
    LOGI("Init step 4/9: OK");
    usleep(100000);

    // Step 5: Refresh all items
    LOGI("Init step 5/9: Refreshing all items");
    uint8_t refresh_payload[] = {0xFF};

    if (send_control_command(fd, 0x00, COMMON_ITEM_REFRESH_REQUEST,
                            refresh_payload, sizeof(refresh_payload)) < 0) {
        LOGE("Init step 5/9: FAILED at COMMON_ITEM_REFRESH");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, LTE_ITEM_REFRESH_REQUEST,
                            refresh_payload, sizeof(refresh_payload)) < 0) {
        LOGE("Init step 5/9: FAILED at LTE_ITEM_REFRESH");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, EDGE_ITEM_REFRESH_REQUEST,
                            refresh_payload, sizeof(refresh_payload)) < 0) {
        LOGE("Init step 5/9: FAILED at EDGE_ITEM_REFRESH");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, HSPA_ITEM_REFRESH_REQUEST,
                            refresh_payload, sizeof(refresh_payload)) < 0) {
        LOGE("Init step 5/9: FAILED at HSPA_ITEM_REFRESH");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, CDMA_ITEM_REFRESH_REQUEST,
                            refresh_payload, sizeof(refresh_payload)) < 0) {
        LOGE("Init step 5/9: FAILED at CDMA_ITEM_REFRESH");
        return -1;
    }
    LOGI("Init step 5/9: OK");
    usleep(100000);

    // Step 6: Stop TRACE (not needed for most use cases)
    LOGI("Init step 6/9: Stopping TRACE");
    uint8_t trace_payload_1[] = {0x01};
    uint8_t trace_payload_2[] = {0x02};

    if (send_control_command(fd, 0x00, TRACE_STOP_REQUEST,
                            trace_payload_1, sizeof(trace_payload_1)) < 0) {
        LOGE("Init step 6/9: FAILED at TRACE_STOP (1)");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, TRACE_STOP_REQUEST,
                            trace_payload_2, sizeof(trace_payload_2)) < 0) {
        LOGE("Init step 6/9: FAILED at TRACE_STOP (2)");
        return -1;
    }
    usleep(50000);

    // Send again
    if (send_control_command(fd, 0x00, TRACE_STOP_REQUEST,
                            trace_payload_1, sizeof(trace_payload_1)) < 0) {
        LOGE("Init step 6/9: FAILED at TRACE_STOP (3)");
        return -1;
    }
    usleep(50000);

    if (send_control_command(fd, 0x00, TRACE_STOP_REQUEST,
                            trace_payload_2, sizeof(trace_payload_2)) < 0) {
        LOGE("Init step 6/9: FAILED at TRACE_STOP (4)");
        return -1;
    }
    LOGI("Init step 6/9: OK");
    usleep(100000);

    // Step 7: Stop ILM
    LOGI("Init step 7/9: Stopping ILM");
    if (send_control_command(fd, 0x00, ILM_STOP_REQUEST, NULL, 0) < 0) {
        LOGE("Init step 7/9: FAILED");
        return -1;
    }
    LOGI("Init step 7/9: OK");
    usleep(100000);

    // Step 8: Request trigger table
    LOGI("Init step 8/9: Requesting trigger table");
    if (send_control_command(fd, 0x00, TRIGGER_TABLE_REQUEST, NULL, 0) < 0) {
        LOGE("Init step 8/9: FAILED");
        return -1;
    }
    LOGI("Init step 8/9: OK");
    usleep(100000);

    // Step 9: Enable TCP/IP dump
    LOGI("Init step 9/9: Enabling TCP/IP dump");
    uint8_t tcpip_payload[] = {
        0xDC, 0x05,  // MTU RX = 1500 (little-endian)
        0xDC, 0x05   // MTU TX = 1500 (little-endian)
    };
    if (send_control_command(fd, 0x00, TCPIP_DUMP_REQUEST,
                            tcpip_payload, sizeof(tcpip_payload)) < 0) {
        LOGE("Init step 9/9: FAILED");
        return -1;
    }
    LOGI("Init step 9/9: OK");

    // Calculate initialization time
    gettimeofday(&end_time, NULL);
    double elapsed_ms = ((end_time.tv_sec - start_time.tv_sec) * 1000.0) +
                       ((end_time.tv_usec - start_time.tv_usec) / 1000.0);
    g_last_init_time_ms = elapsed_ms;

    LOGI("=== Modem initialization complete (took %.1f ms) ===", elapsed_ms);

    // Wait for modem to start sending data
    LOGI("Waiting 1 second for modem to start sending data...");
    sleep(1);

    return 0;
}

/*
 * Open Shannon modem device
 * Returns file descriptor on success, -1 on failure
 *
 * Note: Device must be opened with O_RDWR to allow writing initialization
 * commands to the modem. Read-only mode will not allow modem configuration.
 */
static int
open_shannon_device(const char *device_path)
{
    LOGI("Opening Shannon device: %s", device_path);

    // Use O_RDWR to allow writing initialization commands
    int fd = open(device_path, O_RDWR | O_NONBLOCK);
    if (fd < 0) {
        LOGE("Failed to open %s: %s", device_path, strerror(errno));
        return -1;
    }

    LOGI("Shannon device opened successfully (fd=%d, mode=O_RDWR|O_NONBLOCK)", fd);
    return fd;
}

/*
 * Validate Shannon frame structure
 * Returns 0 if valid, negative error code otherwise
 */
static int
validate_shannon_frame(const uint8_t *frame, size_t frame_len)
{
    if (frame_len < SHANNON_MIN_FRAME_SIZE) {
        return -1;  // Too short
    }

    if (frame[0] != SHANNON_START_MARKER) {
        return -2;  // Invalid start marker
    }

    // Parse length1 from header (bytes 1-2, little-endian)
    uint16_t length1 = frame[1] | (frame[2] << 8);

    // Frame size should be length1 + 2 (start + end markers)
    if (frame_len != length1 + 2) {
        return -3;  // Length mismatch
    }

    // Check end marker
    if (frame[frame_len - 1] != SHANNON_END_MARKER) {
        return -4;  // Invalid end marker
    }

    // Parse length2 from header (bytes 4-5, little-endian)
    uint16_t length2 = frame[4] | (frame[5] << 8);

    // Validate length consistency: length2 + 3 should equal length1
    if (length2 + 3 != length1) {
        return -5;  // Inner/outer length mismatch
    }

    // Validate direction field (byte 8)
    uint8_t direction = frame[8];
    if (direction != 0xA0 && direction != 0xA1 && direction != 0xA2) {
        return -6;  // Invalid direction
    }

    return 0;  // Valid frame
}

/*
 * Read from device and buffer the data
 * Returns number of bytes read, 0 if would block, -1 on error
 */
static ssize_t
read_and_buffer(int device_fd, ReadBuffer *rb)
{
    // Shift unconsumed data to beginning of buffer
    if (rb->pos > 0) {
        if (rb->len > rb->pos) {
            memmove(rb->data, rb->data + rb->pos, rb->len - rb->pos);
            rb->len -= rb->pos;
        } else {
            rb->len = 0;
        }
        rb->pos = 0;
    }

    // Check if buffer is full
    if (rb->len >= BUFFER_SIZE) {
        LOGW("Buffer full, discarding oldest data");
        rb->len = BUFFER_SIZE / 2;
        rb->pos = 0;
    }

    // Read new data into buffer
    ssize_t n = read(device_fd, rb->data + rb->len, BUFFER_SIZE - rb->len);

    if (n < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            // No data available, not an error
            return 0;
        }
        if (errno == EINTR) {
            // Interrupted by signal, try again
            return 0;
        }
        LOGE("Read error from device: %s", strerror(errno));
        return -1;
    }

    if (n == 0) {
        // For non-blocking character devices, read() returning 0 means no data available
        // This is not an error, just return 0 to indicate no data read
        return 0;
    }

    rb->len += n;
    return n;
}

/*
 * Extract next complete Shannon frame from buffer
 * Returns 1 if frame extracted, 0 if need more data, -1 on error
 * On success, frame_out contains the complete frame including delimiters
 */
static int
extract_next_frame(ReadBuffer *rb, uint8_t *frame_out, size_t *frame_len_out)
{
    while (rb->pos < rb->len) {
        // Find start marker (0x7F)
        uint8_t *start = memchr(rb->data + rb->pos, SHANNON_START_MARKER,
                                rb->len - rb->pos);

        if (!start) {
            // No start marker found, consume all data
            rb->pos = rb->len;
            return 0;  // Need more data
        }

        size_t frame_start = start - rb->data;

        // Check if we have enough data for minimal frame (start + header + end)
        if (rb->len - frame_start < SHANNON_MIN_FRAME_SIZE) {
            rb->pos = frame_start;
            return 0;  // Need more data
        }

        // Parse length1 from header (bytes 1-2 after start marker)
        uint16_t length1 = start[1] | (start[2] << 8);

        // Validate length (sanity check)
        if (length1 < 11 || length1 > (MAX_FRAME_SIZE - 2)) {
            // Invalid length, skip this byte and continue searching
            rb->pos = frame_start + 1;
            LOGD("Invalid length1=%u at offset %zu, resyncing", length1, frame_start);
            continue;
        }

        // Calculate total frame size (length1 + start + end markers)
        size_t frame_size = length1 + 2;

        // Check if we have complete frame
        if (rb->len - frame_start < frame_size) {
            rb->pos = frame_start;
            return 0;  // Need more data
        }

        // Verify end marker
        if (start[length1 + 1] != SHANNON_END_MARKER) {
            rb->pos = frame_start + 1;
            LOGD("Invalid end marker at offset %zu, resyncing", frame_start);
            continue;
        }

        // Validate header consistency (length2 + 3 should equal length1)
        uint16_t length2 = start[4] | (start[5] << 8);
        if (length2 + 3 != length1) {
            rb->pos = frame_start + 1;
            LOGD("Length mismatch: length2=%u, length1=%u at offset %zu, resyncing",
                 length2, length1, frame_start);
            continue;
        }

        // Frame looks valid, copy it
        memcpy(frame_out, start, frame_size);
        *frame_len_out = frame_size;
        rb->pos = frame_start + frame_size;

        // Final validation
        int validation_result = validate_shannon_frame(frame_out, frame_size);
        if (validation_result != 0) {
            LOGW("Frame validation failed with code %d, skipping", validation_result);
            continue;
        }

        return 1;  // Frame extracted successfully
    }

    return 0;  // Need more data
}

/*
 * Write log message to FIFO in DiagRevealer format
 * Returns 0 on success, -1 on failure
 */
static int
write_to_fifo(int fifo_fd, const uint8_t *frame, size_t frame_len)
{
    // Calculate total message length (timestamp + frame)
    uint16_t msg_length = 8 + frame_len;

    // Prepare header
    uint16_t msg_type = htole16(FIFO_MSG_TYPE_LOG);
    uint16_t msg_length_le = htole16(msg_length);

    // Get current timestamp
    double timestamp = get_posix_timestamp();

    // Write message type
    ssize_t ret = write(fifo_fd, &msg_type, sizeof(uint16_t));
    if (ret != sizeof(uint16_t)) {
        LOGE("Failed to write message type to FIFO: %s", strerror(errno));
        return -1;
    }

    // Write message length
    ret = write(fifo_fd, &msg_length_le, sizeof(uint16_t));
    if (ret != sizeof(uint16_t)) {
        LOGE("Failed to write message length to FIFO: %s", strerror(errno));
        return -1;
    }

    // Write timestamp
    ret = write(fifo_fd, &timestamp, sizeof(double));
    if (ret != sizeof(double)) {
        LOGE("Failed to write timestamp to FIFO: %s", strerror(errno));
        return -1;
    }

    // Write frame (including delimiters)
    ret = write(fifo_fd, frame, frame_len);
    if (ret != (ssize_t)frame_len) {
        LOGE("Failed to write frame to FIFO: %s", strerror(errno));
        return -1;
    }

    return 0;
}

/*
 * Write START_LOG_FILE message to FIFO
 */
static int
write_fifo_start_message(int fifo_fd, const char *filename)
{
    uint16_t msg_type = htole16(FIFO_MSG_TYPE_START_LOG_FILE);
    uint16_t msg_length = htole16(strlen(filename));

    if (write(fifo_fd, &msg_type, sizeof(uint16_t)) != sizeof(uint16_t)) {
        LOGE("Failed to write START message type: %s", strerror(errno));
        return -1;
    }

    if (write(fifo_fd, &msg_length, sizeof(uint16_t)) != sizeof(uint16_t)) {
        LOGE("Failed to write START message length: %s", strerror(errno));
        return -1;
    }

    if (write(fifo_fd, filename, strlen(filename)) != (ssize_t)strlen(filename)) {
        LOGE("Failed to write START filename: %s", strerror(errno));
        return -1;
    }

    LOGI("Sent START_LOG_FILE: %s", filename);
    return 0;
}

/*
 * Write END_LOG_FILE message to FIFO
 */
static int
write_fifo_end_message(int fifo_fd, const char *filename)
{
    uint16_t msg_type = htole16(FIFO_MSG_TYPE_END_LOG_FILE);
    uint16_t msg_length = htole16(strlen(filename));

    if (write(fifo_fd, &msg_type, sizeof(uint16_t)) != sizeof(uint16_t)) {
        LOGE("Failed to write END message type: %s", strerror(errno));
        return -1;
    }

    if (write(fifo_fd, &msg_length, sizeof(uint16_t)) != sizeof(uint16_t)) {
        LOGE("Failed to write END message length: %s", strerror(errno));
        return -1;
    }

    if (write(fifo_fd, filename, strlen(filename)) != (ssize_t)strlen(filename)) {
        LOGE("Failed to write END filename: %s", strerror(errno));
        return -1;
    }

    LOGI("Sent END_LOG_FILE: %s", filename);
    return 0;
}

/*
 * Main function
 */
int
main(int argc, char **argv)
{
    int ret = 0;
    const char *device_path = DEFAULT_DEVICE_PATH;
    const char *fifo_path = NULL;
    const char *config_file = NULL;

    LOGI("Shannon Revealer v%s starting", SHANNON_REVEALER_VERSION);

    // Parse command-line arguments
    if (argc < 3) {
        printf("Shannon Revealer v%s\n", SHANNON_REVEALER_VERSION);
        printf("Based on diag_revealer by Yuanjie Li, Jiayao Li, Ruihan Li\n\n");
        printf("Usage: %s <config_file> <fifo_path> [device_path]\n", argv[0]);
        printf("  config_file:  Configuration file path (currently unused)\n");
        printf("  fifo_path:    FIFO pipe path for output\n");
        printf("  device_path:  Shannon device path (default: %s)\n", DEFAULT_DEVICE_PATH);
        printf("\n");
        printf("Example:\n");
        printf("  %s /dev/null /data/local/tmp/diag_revealer_fifo\n", argv[0]);
        printf("  %s /dev/null /data/local/tmp/diag_revealer_fifo /dev/umts_ipc0\n", argv[0]);
        return 0;
    }

    config_file = argv[1];  // Currently unused, but accept for compatibility
    fifo_path = argv[2];

    if (argc >= 4) {
        device_path = argv[3];
    }

    LOGI("Config file: %s (ignored)", config_file);
    LOGI("FIFO path: %s", fifo_path);
    LOGI("Device path: %s", device_path);

    // Setup signal handlers
    if (signal(SIGINT, signal_handler) == SIG_ERR) {
        LOGW("Failed to register SIGINT handler");
    }
    if (signal(SIGTERM, signal_handler) == SIG_ERR) {
        LOGW("Failed to register SIGTERM handler");
    }
    if (signal(SIGPIPE, signal_handler) == SIG_ERR) {
        LOGW("Failed to register SIGPIPE handler");
    }

    // Open Shannon device
    g_device_fd = open_shannon_device(device_path);
    if (g_device_fd < 0) {
        LOGE("Cannot open Shannon device, exiting");
        return -1;
    }

    // Initialize modem diagnostic mode
    LOGI("Initializing Shannon modem diagnostic mode...");
    if (initialize_shannon_modem(g_device_fd) < 0) {
        LOGE("Modem initialization failed, exiting");
        g_init_failure_count++;
        close(g_device_fd);
        return -1;
    }
    g_init_success_count++;
    LOGI("Modem initialized successfully, ready to capture data");
    LOGI("Initialization took %.1f ms", g_last_init_time_ms);

    // Open FIFO for writing (blocks until reader opens)
    LOGI("Opening FIFO: %s (will block until reader connects)", fifo_path);
    g_fifo_fd = open(fifo_path, O_WRONLY);
    if (g_fifo_fd < 0) {
        LOGE("Failed to open FIFO %s: %s", fifo_path, strerror(errno));
        close(g_device_fd);
        return -1;
    }
    LOGI("FIFO opened successfully (fd=%d)", g_fifo_fd);

    // Send START_LOG_FILE message
    char start_filename[256];
    snprintf(start_filename, sizeof(start_filename), "shannon_%ld.qmdl", time(NULL));
    write_fifo_start_message(g_fifo_fd, start_filename);

    // Initialize read buffer
    ReadBuffer rb = {0};

    // Statistics
    unsigned long frame_count = 0;
    unsigned long error_count = 0;
    unsigned long bytes_read = 0;
    time_t last_stats_time = time(NULL);

    LOGI("Entering main loop");

    // Main loop
    while (!g_shutdown) {
        // Read from device
        ssize_t n = read_and_buffer(g_device_fd, &rb);
        if (n < 0) {
            LOGE("Read error, exiting");
            ret = -1;
            break;
        }
        if (n > 0) {
            bytes_read += n;
        }

        // Extract and process frames
        while (!g_shutdown) {
            uint8_t frame[MAX_FRAME_SIZE];
            size_t frame_len;

            int extract_result = extract_next_frame(&rb, frame, &frame_len);

            if (extract_result < 0) {
                // Error
                LOGE("Frame extraction error");
                error_count++;
                break;
            } else if (extract_result == 0) {
                // Need more data
                break;
            } else if (extract_result == 1) {
                // Frame extracted successfully
                frame_count++;

                // Write to FIFO
                if (write_to_fifo(g_fifo_fd, frame, frame_len) < 0) {
                    LOGE("Failed to write frame to FIFO, exiting");
                    ret = -1;
                    g_shutdown = 1;
                    break;
                }

                // Log frame details periodically
                if (frame_count % 100 == 0) {
                    uint8_t group = frame[9] & 0x1F;
                    uint8_t command = frame[10];
                    LOGD("Frame #%lu: len=%zu, group=0x%02x, cmd=0x%02x",
                         frame_count, frame_len, group, command);
                }
            }
        }

        // Print statistics every 10 seconds
        time_t now = time(NULL);
        if (now - last_stats_time >= 10) {
            LOGI("Statistics: frames=%lu, errors=%lu, bytes_read=%lu",
                 frame_count, error_count, bytes_read);
            last_stats_time = now;
        }

        // Sleep briefly to avoid busy-waiting when no data available
        if (n == 0) {
            usleep(10000);  // 10ms
        }
    }

    LOGI("Shutting down");

    // Send END_LOG_FILE message
    write_fifo_end_message(g_fifo_fd, start_filename);

    // Print final statistics
    LOGI("Final statistics: frames=%lu, errors=%lu, bytes_read=%lu",
         frame_count, error_count, bytes_read);
    LOGI("Initialization statistics: successes=%lu, failures=%lu, last_init_time=%.1f ms",
         g_init_success_count, g_init_failure_count, g_last_init_time_ms);

    // Cleanup
    if (g_device_fd >= 0) {
        close(g_device_fd);
        LOGI("Shannon device closed");
    }

    if (g_fifo_fd >= 0) {
        close(g_fifo_fd);
        LOGI("FIFO closed");
    }

    LOGI("Shannon Revealer exiting with code %d", ret);
    return ret;
}
