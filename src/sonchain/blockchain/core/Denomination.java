package sonchain.blockchain.core;

import java.math.BigInteger;

public enum Denomination {

    WEI(NewBigInt(0)),
    SZABO(NewBigInt(12)),
    FINNEY(NewBigInt(15)),
    ETHER(NewBigInt(18));

    private BigInteger m_amount = BigInteger.ZERO;

    private Denomination(BigInteger value) {
        this.m_amount = value;
    }

    public BigInteger Value() {
        return m_amount;
    }

    public long LongValue() {
        return Value().longValue();
    }

    private static BigInteger NewBigInt(int value) {
        return BigInteger.valueOf(10).pow(value);
    }

    public static String ToFriendlyString(BigInteger value) {
        if (value.compareTo(ETHER.Value()) == 1 || value.compareTo(ETHER.Value()) == 0) {
            return Float.toString(value.divide(ETHER.Value()).floatValue()) +  " ETHER";
        }
        else if(value.compareTo(FINNEY.Value()) == 1 || value.compareTo(FINNEY.Value()) == 0) {
            return Float.toString(value.divide(FINNEY.Value()).floatValue()) +  " FINNEY";
        }
        else if(value.compareTo(SZABO.Value()) == 1 || value.compareTo(SZABO.Value()) == 0) {
            return Float.toString(value.divide(SZABO.Value()).floatValue()) +  " SZABO";
        }
        else{
            return Float.toString(value.divide(WEI.Value()).floatValue()) +  " WEI";
        }
    }
}
