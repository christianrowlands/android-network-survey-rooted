package com.craxiom.networksurveyplus.parser;

import com.craxiom.networksurveyplus.util.ParserUtils;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

/**
 * Handles reassembly of multi-chunk Shannon RRC messages.
 * Shannon command 0x54 splits large RRC messages into multiple chunks.
 */
public class ShannonMessageChunker
{
    // Map: msgid → ChunkSet
    private final Map<Integer, ChunkSet> chunkStorage = new ConcurrentHashMap<>();

    /**
     * Adds a chunk and attempts reassembly.
     *
     * @param payload Shannon multi-chunk payload (7-byte header + chunk data)
     * @return Complete RRC message if all chunks received, null otherwise
     */
    public byte[] addChunk(byte[] payload)
    {
        if (payload == null || payload.length < 8)
        {
            Timber.w("Shannon chunk payload too short: %d bytes", payload != null ? payload.length : 0);
            return null;
        }

        try
        {
            // Parse chunk header (7 bytes)
            int totalChunks = payload[0] & 0xFF;
            int numChunk = payload[1] & 0xFF;  // 1-indexed
            int msgid = payload[2] & 0xFF;
            int channel = payload[3] & 0xFF;
            int direction = payload[4] & 0xFF;
            int length = ParserUtils.getShort(payload, 5, ByteOrder.LITTLE_ENDIAN) & 0xFFFF;

            // Extract chunk data
            byte[] chunkData = new byte[payload.length - 7];
            System.arraycopy(payload, 7, chunkData, 0, chunkData.length);

            Timber.v("Shannon chunk: msgid=%d, chunk %d/%d, channel=%d, direction=%d, length=%d",
                    msgid, numChunk, totalChunks, channel, direction, length);

            // Get or create chunk set
            ChunkSet chunkSet = chunkStorage.computeIfAbsent(msgid,
                    k -> new ChunkSet(totalChunks, channel, direction));

            // Validate consistency
            if (chunkSet.totalChunks != totalChunks)
            {
                Timber.w("Chunk count mismatch for msgid %d: expected %d, got %d",
                        msgid, chunkSet.totalChunks, totalChunks);
                chunkStorage.remove(msgid);
                return null;
            }

            // Store chunk
            chunkSet.addChunk(numChunk, chunkData);

            // Check if complete
            if (chunkSet.isComplete())
            {
                byte[] completeMessage = chunkSet.reassemble();
                chunkStorage.remove(msgid);  // Clean up

                Timber.i("Shannon RRC reassembled: msgid=%d, %d chunks, %d bytes",
                        msgid, totalChunks, completeMessage.length);

                return completeMessage;
            }

            return null;  // Waiting for more chunks
        } catch (Exception e)
        {
            Timber.e(e, "Error processing Shannon chunk");
            return null;
        }
    }

    /**
     * Clears stale chunks (for cleanup/timeout).
     * Call periodically to prevent memory leaks from incomplete messages.
     */
    public void clearStaleChunks()
    {
        // TODO: Implement timeout-based cleanup if needed
        // For now, chunks are cleaned up after successful reassembly
    }

    /**
     * Stores chunks for a single message.
     */
    private static class ChunkSet
    {
        final int totalChunks;
        final int channel;
        final int direction;
        final Map<Integer, byte[]> chunks;  // chunk_number → data

        ChunkSet(int totalChunks, int channel, int direction)
        {
            this.totalChunks = totalChunks;
            this.channel = channel;
            this.direction = direction;
            this.chunks = new HashMap<>();
        }

        void addChunk(int chunkNumber, byte[] data)
        {
            chunks.put(chunkNumber, data);
        }

        boolean isComplete()
        {
            // Check if we have all chunks (1 through totalChunks)
            for (int i = 1; i <= totalChunks; i++)
            {
                if (!chunks.containsKey(i))
                {
                    return false;
                }
            }
            return true;
        }

        byte[] reassemble()
        {
            // Build RRC header (4 bytes: channel, direction, length)
            // Then concatenate all chunks in order

            // Calculate total length
            int totalLength = 0;
            for (int i = 1; i <= totalChunks; i++)
            {
                totalLength += chunks.get(i).length;
            }

            // Create buffer: 4-byte header + all chunk data
            byte[] result = new byte[4 + totalLength];

            // Write Shannon RRC header
            result[0] = (byte) channel;
            result[1] = (byte) direction;
            result[2] = (byte) (totalLength & 0xFF);        // length low byte
            result[3] = (byte) ((totalLength >> 8) & 0xFF); // length high byte

            // Concatenate chunks in order
            int offset = 4;
            for (int i = 1; i <= totalChunks; i++)
            {
                byte[] chunkData = chunks.get(i);
                System.arraycopy(chunkData, 0, result, offset, chunkData.length);
                offset += chunkData.length;
            }

            return result;
        }
    }
}
