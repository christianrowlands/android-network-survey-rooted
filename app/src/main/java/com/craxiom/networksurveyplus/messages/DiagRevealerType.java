package com.craxiom.networksurveyplus.messages;

import org.codehaus.preon.annotation.BoundEnumOption;

/**
 * As a part of the {@link PayloadDataMessage}, the PCS
 * will report a status for each power amplifier with 1 byte. In that byte, bits [0-4] represent the power amp state where
 * the value corresponds to the enumerated types in this class.
 * <p>
 * Using Preon, we can bind a specific parsed value to an enumeration value using the {@link BoundEnumOption} annotation.
 *
 * @since 0.1.0
 */
public enum DiagRevealerType
{
    @BoundEnumOption(0)
    UNKNOWN(0),
    @BoundEnumOption(1)
    LOG(1),
    @BoundEnumOption(2)
    START_LOG_FILE(2),
    @BoundEnumOption(3)
    END_LOG_FILE(3);

    private static final DiagRevealerType[] VALUES = values();

    final int value;

    DiagRevealerType(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    public static DiagRevealerType fromValue(int value)
    {
        for (DiagRevealerType type : VALUES)
        {
            if (type.value == value)
            {
                return type;
            }
        }

        return UNKNOWN;
    }
}