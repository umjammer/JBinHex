/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.gjt.convert.binhex;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * RLE_CRCInputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/11 umjammer initial version <br>
 */
class RLE_CRCInputStreamTest {

    @Test
    void test() {
        fail("Not yet implemented");
    }

    public static void main(String[] args) {
        try (InputStream in = new RLE_CRCInputStream(System.in)) {
            byte[] buf = new byte[1024];

            System.err.println("Starting to convert");
            while (true) {
                int r = in.read(buf);
                if (r <= 0)
                    return;
                System.out.write(buf, 0, r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/* */
