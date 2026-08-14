package com.doktorarbeitbeham;

public class Constants {
    // Configured via local.properties (uploadAddress) — see local.properties.example
    public final static String UPLOAD_ADDRESS = BuildConfig.UPLOAD_ADDRESS;
    public final static int BATCH_SIZE_DEFAULT = 10;
    public final static int MAX_TO_SEND_DEFAULT= 0;
    public final static int MAX_BATCHES_TO_SEND = 10;
    public final static int REQ_TIMEOUT = 1200;
}
