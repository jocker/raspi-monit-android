package rpi.aut.rpi_monit.timeseries;

public class TimeSeries {

    private int[] mPoints;
    private int mSize;
    private long mMinTs;

    private int mStepSize, mSeriesMin = Integer.MAX_VALUE, mSeriesMax = Integer.MIN_VALUE;
    private final int mEmptyPointValue;

    public TimeSeries(int emptyValue){
        this(emptyValue, null);
    }

    public TimeSeries(int emptyPointValue, TimeSeriesData data){
        mEmptyPointValue = emptyPointValue;
        int capacity = Math.max(data == null || data.points == null ? 0 : data.points.length, 100);
        mPoints = new int[capacity];
        add(data);
    }

    public int add(TimeSeriesData other){

        if(other == null || other.points == null || other.points.length == 0 || (mStepSize != 0 && other.stepSize != mStepSize)){
            return -1;
        }

        if(mStepSize == 0){
            mStepSize = other.stepSize;
        }

        mSeriesMax = Math.max(mSeriesMax, other.seriesMax);
        mSeriesMin = Math.min(mSeriesMin, other.seriesMin);
        return add(other.timestamp, other.points);
    }

    public int add(long timestamp, int... dataValues){
        int[] values = dataValues;
        if(mStepSize == 0){
            return -1;
        }

        if(dataValues.length == 0){
            return -1;
        }

        if(mMinTs == 0){
            mMinTs = timestamp;
        }

        int pos = getPositionForTimestamp(timestamp);
        int len = dataValues.length;

        if(mSize > 0 && pos < mSize){
            throw new RuntimeException("invalid point added");
        }

        ensureCapacity(pos+len);
        int insertPosition = mSize;
        if(mSize > 0){
            for(int i=mSize; i< pos; i++){
                mPoints[i] = mEmptyPointValue;
            }
        }

        System.arraycopy(dataValues, 0, mPoints, pos, len);
        mSize += (pos-mSize)+ len;

        return insertPosition;
    }

    private void ensureCapacity(int minSize){
        int oldLen = mPoints.length;
        if(oldLen < minSize){
            int newSize = minSize + 100;
            int[] newValues = new int[newSize];
            System.arraycopy(mPoints, 0, newValues, 0, oldLen);
            mPoints = newValues;
        }
    }

    public boolean trimToSize(int size){
        if(mSize > size){
            int toRemove = mSize - size;
            int[] points = new int[size+100];
            System.arraycopy(mPoints, toRemove-1, points, 0, size);
            mPoints = points;
            mSize = size;
            mMinTs += toRemove*mStepSize;
            return true;
        }
        return false;
    }

    public int getCount(){
        return mSize;
    }

    public boolean isEmpty(){
        return getCount() == 0;
    }

    private int getPositionForTimestamp(long timestamp){
        /*if(timestamp < mMinTs){
            return -1;
        }
        return (int)((timestamp-mMinTs)/mStepSize);*/
        return mSize;
    }

    public int getValue(int position){
        return mPoints[position];
    }

    public long getTimestamp(int position){
        return mMinTs+position*mStepSize;
    }


}
