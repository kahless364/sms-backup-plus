package com.fsck.k9.mail.store.imap;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import com.beetstra.jutf7.CharsetProvider;


class FolderNameCodec {
    private final Charset modifiedUtf7Charset;


    public static FolderNameCodec newInstance() {
        return new FolderNameCodec();
    }

    private FolderNameCodec() {
        modifiedUtf7Charset = new CharsetProvider().charsetForName("X-RFC-3501");
    }

    public String encode(String folderName) {
        ByteBuffer byteBuffer = modifiedUtf7Charset.encode(folderName);
        byte[] bytes = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes);

        try {
            return new String(bytes, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String decode(String encodedFolderName) throws CharacterCodingException {
        CharsetDecoder decoder = modifiedUtf7Charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encodedFolderName.getBytes("US-ASCII"));
            CharBuffer charBuffer = decoder.decode(byteBuffer);
            return charBuffer.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
