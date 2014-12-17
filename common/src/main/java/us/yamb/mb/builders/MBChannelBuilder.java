package us.yamb.mb.builders;

public interface MBChannelBuilder<T>
{
    MBChannelBuilder<T> name(String name);
    T build();
}
