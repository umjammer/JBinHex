/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.gjt.convert.binhex;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * BinHex4InputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/11 umjammer initial version <br>
 */
class BinHex4InputStreamTest {

    @Test
    void test() {
        fail("Not yet implemented");
    }

    public static void main(String[] args) {
        try (BinHex4InputStream in = new BinHex4InputStream(System.in)) {
            System.err.println(in.getHeader());

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
