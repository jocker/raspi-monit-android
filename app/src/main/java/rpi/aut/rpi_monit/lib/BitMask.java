package rpi.aut.rpi_monit.lib;

public class BitMask {

    public static final int BOOLEAN_SHIFT = 30,
            BOOLEAN_MASK =          0x3 <<  BOOLEAN_SHIFT,
            BOOLEAN_UNSPECIFIED =   0   <<  BOOLEAN_SHIFT,
            BOOLEAN_TRUE =          1   <<  BOOLEAN_SHIFT,
            BOOLEAN_FALSE =         2   <<  BOOLEAN_SHIFT;

    public static int addFlags(int dest, int... flags){
        return dest | mergeFlags(flags);
    }

    public static int removeFlags(int dest, int... flags){
        return dest & ~(mergeFlags(flags));
    }

    public static int makeMask(int leftmostBit, int maskWidth) {
        return 0xFFFFFFFF >>> (32-maskWidth) << (leftmostBit-maskWidth);
    }

    public static String stringifyMask(int mask_32_bit){
        return String.format("%32s", Integer.toBinaryString(mask_32_bit)).replace(' ','0');
    }

    public static boolean contains(int src, int what){
        return (src & what) == what;
    }


    public static int makeBoolean(int num, boolean value){
        int mode = value ? BOOLEAN_TRUE : BOOLEAN_FALSE;
        return (num & ~BOOLEAN_MASK) | (mode & BOOLEAN_MASK);
    }

    public static boolean getBoolean(int spec, boolean defaultValue){
        int mode = spec & BOOLEAN_MASK;
        return mode == BOOLEAN_UNSPECIFIED ? defaultValue : mode == BOOLEAN_TRUE;
    }

    public static int getValue(int spec){
        return spec & ~BOOLEAN_MASK;
    }

    private int mFlags;

    public static int mergeFlags(int ...flags){
        int res = 0;
        for(int flag: flags){
            res |= flag;
        }
        return res;
    }

    public static int sanitizeFlags(int actual, int...allowed){
        return actual & mergeFlags(allowed);
    }

    public static BitMask from(BitMask other){
        return new BitMask(other.mFlags);
    }

    public static BitMask from(int ...value){
        return new BitMask(value);
    }

    public BitMask(int ...value){
        mFlags = mergeFlags(value);
    }

    public boolean contains(int ...flags){
        return (mFlags & mergeFlags(flags)) > 0;
    }

    public boolean containsAll(int ...flags){
        if(mFlags <= 0){
            return false;
        }
        int merged = mergeFlags(flags);
        return merged > 0 && ((mFlags & merged) == merged);
    }

    public boolean add(int ...flags){
        int flag =mergeFlags(flags);
        if((mFlags&flag) != flag){
            mFlags |= mergeFlags(flags);
            return true;
        }
        return false;
    }

    public void remove(int ...flags){
        mFlags &= ~mergeFlags(flags);
    }

    public int getValue(){
        return mFlags;
    }

    protected void setValue(int newValue){
        mFlags = newValue;
    }

    public boolean isEmpty(){
        return mFlags == 0;
    }

    public void clear(){
        mFlags = 0;
    }

    @Override
    public String toString(){
        return stringifyMask(mFlags);
    }

    public static class Sequence extends BitMask {

        private int mAllowedFlags;

        public Sequence(int value, int ...allowedFlags){
            super(value);
            mAllowedFlags = mergeFlags(allowedFlags);
        }

        @Override
        public boolean add(int ...flags){
            // only adds the flags if they are bigger than the biggest flag we have
            boolean flagsValid = false;
            for(int flag: flags){
                flagsValid = flag > highest() && Integer.highestOneBit(flag) == Integer.lowestOneBit(flag);
                if(!flagsValid){
                    break;
                }
            }
            return flagsValid && super.add(flags);
        }

        public int highest(){
            return Integer.highestOneBit(getValue());
        }

        public int lowest(){
            return Integer.lowestOneBit(getValue());
        }

        public boolean eq(int flag){
            return highest() == flag;
        }

        public boolean gt(int flag){
            return highest() > flag && contains(flag);
        }

        public boolean gte(int flag){
            return eq(flag) || gt(flag);
        }

        public void revertTo(int highest){
            if(highest == 0){
                setValue(0);
                return;
            }
            int flags = getValue() | highest;
            for(;;){
                int maxFlag = Integer.highestOneBit(flags);
                if(maxFlag > highest){
                    flags &= ~maxFlag;
                }else{
                    break;
                }
            }
            setValue(flags);
        }

    }
}