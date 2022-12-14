package de.androidcrypto.nfchcendef4;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class MyHostApduService extends HostApduService {

    enum CardSelect {
        SELECT_NONE,
        SELECT_NDEFAPP,
        SELECT_CCFILE,
        SELECT_NDEFFILE,
    }

    final static int APDU_CLA = 0;
    final static int APDU_INS = 1;
    final static int APDU_P1 = 2;
    final static int APDU_P2 = 3;
    final static int APDU_SELECT_LC = 4;
    final static int APDU_SELECT_DATA = 5;
    final static int APDU_READ_LE = 4;

    final static int FILEID_CC = 0xe103;
    final static int FILEID_NDEF = 0xe104;

    final static byte INS_SELECT = (byte)0xa4;
    final static byte INS_READ = (byte)0xb0;

    final static byte P1_SELECT_BY_NAME = (byte)0x04;
    final static byte P1_SELECT_BY_ID = (byte)0x00;

    final static int DATA_OFFSET = 5;
    final static byte[] DATA_SELECT_NDEF = { (byte)0xd2, (byte)0x76, (byte)0x00, (byte)0x00, (byte)0x85, (byte)0x01, (byte)0x01 };

    final static byte[] RET_COMPLETE = { (byte)0x90, (byte)0x00 };
    final static byte[] RET_NONDEF = { (byte)0x6a, (byte)0x82 };

    final static byte[] FILE_CC = {
            (byte)0x00, (byte)0x0f,     //LEN
            (byte)0x20,                 //Mapping Version
            (byte)0x00, (byte)0x40,     //MLe
            (byte)0x00, (byte)0x40,     //MLc

            (byte)0x04,                 //Tag
            (byte)0x06,                 //LEN
            (byte)0xe1, (byte)0x04,     //signature
            (byte)0x00, (byte)0x32,     //max ndef size
            (byte)0x00,                 //read access permission
            (byte)0x00,                 //write access permission
    };


    private final static String TAG = "HostSampleService";

    private CardSelect mCardSelect = CardSelect.SELECT_NONE;
    private boolean mSelectNdef = false;
    private byte[] mNdefFile = null;

    public MyHostApduService() {
        super();
        System.out.println("+++++++++++++++++++++++++++++++++++");
        //create ndef file
        //NdefMessage ndef = createUriMessage(3, "hiro99ma.blogspot.com/");
        NdefMessage ndef = createUriMessage(3, "hiro99ma.blogspot.com/");

        byte[] ndefarray = ndef.toByteArray();
        mNdefFile = new byte[2 + ndefarray.length];

        //NLEN
        mNdefFile[0] = (byte)((ndefarray.length & 0xff00) >> 8);
        mNdefFile[1] = (byte)(ndefarray.length  & 0x00ff);
        //NDEF message
        System.arraycopy(ndefarray, 0, mNdefFile, 2, ndefarray.length);
        System.out.println("NDEF-message: " + new String(mNdefFile));
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "onDeactivated");
        mCardSelect = CardSelect.SELECT_NONE;
        mSelectNdef = false;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        boolean ret = false;
        byte[] retData = null;
        System.out.println("+++++++++++++++++++++++++++++++++++");

        switch (commandApdu[APDU_INS]) {
            case INS_SELECT:

                switch (commandApdu[APDU_P1]) {
                    case P1_SELECT_BY_NAME:
                        Log.d(TAG, "select : name");
                        // 1. NDEF Tag Application Select
                        if (memCmp(commandApdu, DATA_OFFSET, DATA_SELECT_NDEF, 0, commandApdu[APDU_SELECT_LC])) {
                            //select NDEF application
                            Log.d(TAG, "select NDEF application");
                            mSelectNdef = true;
                            ret = true;
                        } else {
                            Log.e(TAG, "select: fail");
                        }
                        break;

                    case P1_SELECT_BY_ID:
                        Log.d(TAG, "select : id");
                        if (mSelectNdef) {
                            int file_id = 0;
                            for (int loop = 0; loop < commandApdu[APDU_SELECT_LC]; loop++) {
                                file_id <<= 8;
                                file_id |= commandApdu[DATA_OFFSET + loop] & 0xff;
                            }
                            switch (file_id) {
                                case FILEID_CC:
                                    Log.d(TAG, "select CC file");
                                    mCardSelect = CardSelect.SELECT_CCFILE;
                                    ret = true;
                                    break;

                                case FILEID_NDEF:
                                    Log.d(TAG, "select NDEF file");
                                    mCardSelect = CardSelect.SELECT_NDEFFILE;
                                    ret = true;
                                    break;

                                default:
                                    Log.e(TAG, "select: unknown file id : " + file_id);
                                    break;
                            }
                        } else {
                            Log.e(TAG, "select: not select NDEF app");
                        }
                        break;

                    default:
                        Log.e(TAG, "select: unknown p1 : " + commandApdu[APDU_P1]);
                        break;
                }
                break;

            case INS_READ:
                Log.d(TAG, "read");
                if (mSelectNdef) {
                    int offset = ((commandApdu[APDU_P1]) << 8) | commandApdu[APDU_P2];
                    byte[] src = null;
                    switch (mCardSelect) {
                        case SELECT_CCFILE:
                            Log.d(TAG, "read cc file");
                            src = FILE_CC;
                            ret = true;
                            break;

                        case SELECT_NDEFFILE:
                            Log.d(TAG, "read ndef file");
                            src = mNdefFile;
                            ret = true;
                            break;

                        default:
                            Log.e(TAG, "read: fail : no select");
                            break;
                    }

                    if (ret) {
                        retData = new byte[commandApdu[APDU_READ_LE] + RET_COMPLETE.length];
                        //read data
                        System.arraycopy(src, offset, retData, 0, commandApdu[APDU_READ_LE]);
                        //complete
                        System.arraycopy(RET_COMPLETE, 0, retData, commandApdu[APDU_READ_LE], RET_COMPLETE.length);
                    }
                    break;
                } else {
                    Log.e(TAG, "read: not select NDEF app");
                }
                break;

            default:
                Log.e(TAG, "unknown INS : " + commandApdu[APDU_INS]);
                break;
        }

        if (ret) {
            if (retData == null) {
                Log.d(TAG, "return complete");
                retData = RET_COMPLETE;
            } else {
                Log.d(TAG, "------------------------------");
                for (byte ch : retData) {
                    Log.d(TAG, Integer.toHexString(ch));
                }
                Log.d(TAG, "------------------------------");
            }
        } else {
            Log.e(TAG, "return no ndef");
            retData = RET_NONDEF;
        }
        return retData;
    }


    private boolean memCmp(final byte[] p1, int offset1, final byte[] p2, int offset2, int cmpLen) {
        final int len = p1.length;
        if ((len < offset1 + cmpLen) || (p2.length < offset2 + cmpLen)) {
            Log.d(TAG, "memCmp fail : " + offset1 + " : " + offset2 + " (" + cmpLen + ")");
            Log.d(TAG, "memCmp fail : " + len + " : " + p2.length);
            return false;
        }

        boolean ret = true;
        for (int loop = 0; loop < cmpLen; loop++) {
            if (p1[offset1 + loop] != p2[offset2 + loop]) {
                Log.d(TAG, "unmatch");
                ret = false;
                break;
            }
        }

        return ret;
    }

    private NdefMessage createUriMessage(int index, String uriBody) {
        try {
            byte[] uriBodyBytes = uriBody.getBytes("UTF-8");
            byte[] payload = new byte[1 + uriBody.length()];
            payload[0] = (byte)index;
            System.arraycopy(uriBodyBytes, 0, payload, 1, uriBodyBytes.length);
            return new NdefMessage(new NdefRecord[] {
                    new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload)
            });
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

