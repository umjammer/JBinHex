/*
  JBinHex
  Copyright (C) 2000, Erwin Bolwidt <ejb@klomp.org>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.convert.binhex;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 Converts a 7-bit encoded binhex4.0 data stream to a 8-bit encoded
 data stream. The 8-bit stream still needs to be split in a data and
 a resource fork, and still needs to have any Run-Length Encoded (RLE) 
 sequences expanded.
 <p>
 This class does not (yet) support segmented files. It
 is however very liberal about whitespace: any is allowed. So you could cat
 all segmented files together and remove the "--- end of part NN ---" and
 the beginning-of-next-part identifier "---" lines from the resulting file
 before decoding.

 @author Erwin Bolwidt
 */
public class Hqx7_to_Hqx8InputStream extends FilterInputStream {

    final static String validChars
        = "!\"#$%&'()*+,-012345689@ABCDEFGHIJKLMNPQRSTUVXYZ[`"
        + "abcdefhijklmpqr";

    final static String binhexHeaderId = "(This file must be converted with BinHex";

    /**
     Although encoded in 7-bit, each value only represents 6 bits. This
     is because not all 7-bit values can be used since they may have special
     meanings to the underlying transport mechanism. Compare Hqx7 with Base64,
     they have the same design goals although Base64 probably achieves them
     better.
     */
    final static byte[] sixBitTable = new byte[256];
    final static byte   invalidEntry = (byte)64;

    /**
     The size of the streambuffer. Could be anything that's not too small.
     Bigger sizes probably improve performance, up to a point.
     */
    final static int    sz_streamBuf = 1024;

    /**
     Initializes six-bit table from the validChars string.
     */
    static {
        // An assertion to see if nobody mistakenly changed the validChars
        // string. This whole class wouldn't work if they had.
        if(validChars.length() != 64)
            throw new IllegalStateException(
                    "Incorrect class, the static validChars entry should "
                    + "be 64 characters long.");
        for(int i = 0; i < sixBitTable.length; i++)
        {
            // 64 is the 'invalid character' value
            sixBitTable[i] = invalidEntry;
        }
        // Now give every valid character the 6-bit
        for(int i = 0, l = validChars.length(); i < l; i++)
        {
            sixBitTable[validChars.charAt(i)] = (byte)i;
        }

    }

    /**
     Constructs a Hqx7_to_Hqx8InputStream that reads from the supplied source
     and converts that source to 8-bit Hqx8 without RLE expansion.
     */
    public Hqx7_to_Hqx8InputStream(InputStream source)
    {
        super(source);
    }

    private void skipHeader() throws IOException
    {
        int b, c = nextStreamByte();

        while(true)
        {
            for(int i = 0, l = binhexHeaderId.length(); c != -1; c = nextStreamByte(), i++)
            {
                if(i == l) {
                    skipHeaderAfterId();
                    return;
                }
                if(c != binhexHeaderId.charAt(i))
                    // Try again at the next line.
                    break;
            }

            if(c == -1)
                throw new EOFException("Couldn't find start of Hqx7 part");

            do {
                b = nextStreamByte();
                if(b == -1)
                    throw new EOFException("Couldn't find start of Hqx7 part");
            } while(b != '\n' && b != '\r');

            c = nextStreamByte();
            // Allow MS-DOS type linebreaks too
            if(c == '\n' && b == '\r')
                c = nextStreamByte();

        }
    }

    private void skipHeaderAfterId() throws IOException
    {
        int b;

        do {
            b = nextStreamByte();
            if(b == -1)
                throw new EOFException("Couldn't find start of Hqx7 part");
        } while(b != '\n' && b != '\r');

        do {
            b = nextStreamByte();
            if(b == -1)
                throw new EOFException("Couldn't find start of Hqx7 part");
        } while(b != ':' && Character.isWhitespace((char)b));

        if(b != ':')
            throw new EOFException("Invalid start of Hqx7 part, no : right after id line");
    }

    /**
     Internal method to get the next physical byte from the superclass
     stream, while keeping buffering consistent.

     @return
        the byte that was read, or -1 if end-of-file was encountered for
        the first time. Any subsequent read throws an EOFException.
     @exception EOFException
        thrown when this method is called after it returned -1 once.
     */
    private int nextStreamByte() throws IOException
    {
        if(sbFilled == -1)
            throw new EOFException("End of file already reached.");

        if(sbIndex < sbFilled)
        {
// Debug line
//            System.err.print((char)streamBuffer[sbIndex]);
            // Take care to return no sign-extended numbers, hence the & 0xff
            return (streamBuffer[sbIndex++]) & 0xff;
        }

        sbFilled = super.read(streamBuffer, 0, streamBuffer.length);
        if(sbFilled <= 0)
        {
            sbFilled = -1;
// Debug line
//            new Exception().printStackTrace();
            return sbFilled;
        }

        sbIndex = 0;
// Debug line
//        System.err.print((char)streamBuffer[sbIndex]);
        // Take care to return no sign-extended numbers, hence the & 0xff
        return (streamBuffer[sbIndex++]) & 0xff;
    }

    private int next6bits() throws IOException
    {
        if(hardEOF)
            throw new EOFException(
                    "All Hqx7 data was read and a soft EOF was already "
                    + "reported with a -1 return to read().");

        int b;
        do {
            b = nextStreamByte();
            // True end-of-file is not allowed, a Hqx7 always ends earlier
            // with a : character.
            if(b == -1)
                throw new EOFException(
                        "EOF reached before closing : character. "
                        + "Possible data corruption.");

            // The high bit could have been used as a parity bit. Better be sure.
            b &= 0x7f;

            // The : character terminates the stream
            if(b == ':') {
                hardEOF = true;
                return -1;
            }

        } while(Character.isWhitespace((char)b));

        int v = sixBitTable[b];
        if(v == invalidEntry)
            throw new IOException(
                    "Illegal character in Hqx7 stream encountered, "
                    + "possible data corruption. ('" + (char)b + "')");
        return v;
    }

    private int nextDecodedByte() throws IOException
    {
        while(bitsLeft < 8)
        {
            int bits = next6bits();
            if(bits == -1)
                return -1;
            bitBuffer = (bitBuffer << 6) | bits;
            bitsLeft += 6;
        }
        // Taking 8 bits out.
        bitsLeft -= 8;
        // Incidentally, the bitBuffer value also needs a shift of the number
        // of bits left after taking 8 bits out.
        return (bitBuffer >>> bitsLeft) & 0xff;
    }

    public int read() throws IOException
    {
        if(!headerDone)
        {
            skipHeader();
            headerDone = true;
        }

        if(seenEOF)
        {
            // If this method is called a second time AFTER a -1, the
            // caller must know for itself and be ready to get an EOFException.
            // So set seenEOF to false again, and nextDecodedByte will throw
            // one of these.
            seenEOF = false;
            return -1;
        }
        return nextDecodedByte();
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        if(!headerDone)
        {
            skipHeader();
            headerDone = true;
        }

        if(seenEOF)
        {
            // If this method is called a second time AFTER a -1, the
            // caller must know for itself and be ready to get an EOFException.
            // So set seenEOF to false again, and nextDecodedByte will throw
            // one of these.
            seenEOF = false;
            return -1;
        }

        for(int i = off, max = off+len; i < max; i++)
        {
            int t = nextDecodedByte();
            if(t == -1) {
                if(i == off)
                    // No data read yet, so safe to return -1
                    return -1;
                seenEOF = true;
                return i - off;
            }
            b[i] = (byte)t;
        }
        return len;
    }


    public static void main(String[] args)
    {
        try (InputStream in = new Hqx7_to_Hqx8InputStream(System.in)) {
            byte[] buf = new byte[1024];

            System.err.println("Starting to convert");
            while(true)
            {
                int r = in.read(buf);
                if(r <= 0)
                    return;
                System.out.write(buf, 0, r);
            }
        } catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     A buffer for a maximum of two times six bits.
     */
    private int     bitBuffer;
    /**
     How many bits are left in the bitBuffer.
     */
    private int     bitsLeft;

    /**
     Bytes that were read from the stream but not all yet processed.
     */
    private byte[]  streamBuffer = new byte[sz_streamBuf];

    /**
     The next byte in the stream buffer to process.
     */
    private int     sbIndex = 0;

    /**
     How many bytes in the buffer are stream data; the buffer could
     be longer than that.
     */
    private int     sbFilled = 0;

    /**
     The read(byte[], int, int) call cannot immediately signal eof with a -1
     when it sees it, since it may already have read some data. So it sets
     this flag, which it checks the next time it is called.
     */
    private boolean seenEOF = false;

    /**
     This flag is set by next6bits after it's seen a : delimiter. Any time
     thereafter that next6bits is called, it will throw EOFException.
     */
    private boolean hardEOF = false;

    /**
     This flag is true when the header has been read
     */
    private boolean headerDone = false;

}
