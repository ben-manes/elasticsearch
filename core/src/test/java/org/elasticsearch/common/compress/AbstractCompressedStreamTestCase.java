/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.compress;

import org.apache.lucene.util.LineFileDocs;
import org.apache.lucene.util.TestUtil;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.test.ESTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Test streaming compression (e.g. used for recovery)
 */
public abstract class AbstractCompressedStreamTestCase extends ESTestCase {

    private final Compressor compressor;

    protected AbstractCompressedStreamTestCase(Compressor compressor) {
        this.compressor = compressor;
    }

    public void testRandom() throws IOException {
        Random r = random();
        for (int i = 0; i < 10; i++) {
            byte bytes[] = new byte[TestUtil.nextInt(r, 1, 100000)];
            r.nextBytes(bytes);
            doTest(bytes);
        }
    }

    public void testRandomThreads() throws Exception {
        final Random r = random();
        int threadCount = TestUtil.nextInt(r, 2, 6);
        Thread[] threads = new Thread[threadCount];
        final CountDownLatch startingGun = new CountDownLatch(1);
        for (int tid=0; tid < threadCount; tid++) {
            final long seed = r.nextLong();
            threads[tid] = new Thread() {
                @Override
                public void run() {
                    try {
                        Random r = new Random(seed);
                        startingGun.await();
                        for (int i = 0; i < 10; i++) {
                            byte bytes[] = new byte[TestUtil.nextInt(r, 1, 100000)];
                            r.nextBytes(bytes);
                            doTest(bytes);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[tid].start();
        }
        startingGun.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    public void testLineDocs() throws IOException {
        Random r = random();
        LineFileDocs lineFileDocs = new LineFileDocs(r);
        for (int i = 0; i < 10; i++) {
            int numDocs = TestUtil.nextInt(r, 1, 200);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (int j = 0; j < numDocs; j++) {
                String s = lineFileDocs.nextDoc().get("body");
                bos.write(s.getBytes(StandardCharsets.UTF_8));
            }
            doTest(bos.toByteArray());
        }
        lineFileDocs.close();
    }

    public void testLineDocsThreads() throws Exception {
        final Random r = random();
        int threadCount = TestUtil.nextInt(r, 2, 6);
        Thread[] threads = new Thread[threadCount];
        final CountDownLatch startingGun = new CountDownLatch(1);
        for (int tid=0; tid < threadCount; tid++) {
            final long seed = r.nextLong();
            threads[tid] = new Thread() {
                @Override
                public void run() {
                    try {
                        Random r = new Random(seed);
                        startingGun.await();
                        LineFileDocs lineFileDocs = new LineFileDocs(r);
                        for (int i = 0; i < 10; i++) {
                            int numDocs = TestUtil.nextInt(r, 1, 200);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            for (int j = 0; j < numDocs; j++) {
                                String s = lineFileDocs.nextDoc().get("body");
                                bos.write(s.getBytes(StandardCharsets.UTF_8));
                            }
                            doTest(bos.toByteArray());
                        }
                        lineFileDocs.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[tid].start();
        }
        startingGun.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    public void testRepetitionsL() throws IOException {
        Random r = random();
        for (int i = 0; i < 10; i++) {
            int numLongs = TestUtil.nextInt(r, 1, 10000);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            long theValue = r.nextLong();
            for (int j = 0; j < numLongs; j++) {
                if (r.nextInt(10) == 0) {
                    theValue = r.nextLong();
                }
                bos.write((byte) (theValue >>> 56));
                bos.write((byte) (theValue >>> 48));
                bos.write((byte) (theValue >>> 40));
                bos.write((byte) (theValue >>> 32));
                bos.write((byte) (theValue >>> 24));
                bos.write((byte) (theValue >>> 16));
                bos.write((byte) (theValue >>> 8));
                bos.write((byte) theValue);
            }
            doTest(bos.toByteArray());
        }
    }

    public void testRepetitionsLThreads() throws Exception {
        final Random r = random();
        int threadCount = TestUtil.nextInt(r, 2, 6);
        Thread[] threads = new Thread[threadCount];
        final CountDownLatch startingGun = new CountDownLatch(1);
        for (int tid=0; tid < threadCount; tid++) {
            final long seed = r.nextLong();
            threads[tid] = new Thread() {
                @Override
                public void run() {
                    try {
                        Random r = new Random(seed);
                        startingGun.await();
                        for (int i = 0; i < 10; i++) {
                            int numLongs = TestUtil.nextInt(r, 1, 10000);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            long theValue = r.nextLong();
                            for (int j = 0; j < numLongs; j++) {
                                if (r.nextInt(10) == 0) {
                                    theValue = r.nextLong();
                                }
                                bos.write((byte) (theValue >>> 56));
                                bos.write((byte) (theValue >>> 48));
                                bos.write((byte) (theValue >>> 40));
                                bos.write((byte) (theValue >>> 32));
                                bos.write((byte) (theValue >>> 24));
                                bos.write((byte) (theValue >>> 16));
                                bos.write((byte) (theValue >>> 8));
                                bos.write((byte) theValue);
                            }
                            doTest(bos.toByteArray());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[tid].start();
        }
        startingGun.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    public void testRepetitionsI() throws IOException {
        Random r = random();
        for (int i = 0; i < 10; i++) {
            int numInts = TestUtil.nextInt(r, 1, 20000);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int theValue = r.nextInt();
            for (int j = 0; j < numInts; j++) {
                if (r.nextInt(10) == 0) {
                    theValue = r.nextInt();
                }
                bos.write((byte) (theValue >>> 24));
                bos.write((byte) (theValue >>> 16));
                bos.write((byte) (theValue >>> 8));
                bos.write((byte) theValue);
            }
            doTest(bos.toByteArray());
        }
    }

    public void testRepetitionsIThreads() throws Exception {
        final Random r = random();
        int threadCount = TestUtil.nextInt(r, 2, 6);
        Thread[] threads = new Thread[threadCount];
        final CountDownLatch startingGun = new CountDownLatch(1);
        for (int tid=0; tid < threadCount; tid++) {
            final long seed = r.nextLong();
            threads[tid] = new Thread() {
                @Override
                public void run() {
                    try {
                        Random r = new Random(seed);
                        startingGun.await();
                        for (int i = 0; i < 10; i++) {
                            int numInts = TestUtil.nextInt(r, 1, 20000);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            int theValue = r.nextInt();
                            for (int j = 0; j < numInts; j++) {
                                if (r.nextInt(10) == 0) {
                                    theValue = r.nextInt();
                                }
                                bos.write((byte) (theValue >>> 24));
                                bos.write((byte) (theValue >>> 16));
                                bos.write((byte) (theValue >>> 8));
                                bos.write((byte) theValue);
                            }
                            doTest(bos.toByteArray());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[tid].start();
        }
        startingGun.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    public void testRepetitionsS() throws IOException {
        Random r = random();
        for (int i = 0; i < 10; i++) {
            int numShorts = TestUtil.nextInt(r, 1, 40000);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            short theValue = (short) r.nextInt(65535);
            for (int j = 0; j < numShorts; j++) {
                if (r.nextInt(10) == 0) {
                    theValue = (short) r.nextInt(65535);
                }
                bos.write((byte) (theValue >>> 8));
                bos.write((byte) theValue);
            }
            doTest(bos.toByteArray());
        }
    }

    public void testMixed() throws IOException {
        Random r = random();
        LineFileDocs lineFileDocs = new LineFileDocs(r);
        for (int i = 0; i < 2; ++i) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int prevInt = r.nextInt();
            long prevLong = r.nextLong();
            while (bos.size() < 400000) {
                switch (r.nextInt(4)) {
                case 0:
                    addInt(r, prevInt, bos);
                    break;
                case 1:
                    addLong(r, prevLong, bos);
                    break;
                case 2:
                    addString(lineFileDocs, bos);
                    break;
                case 3:
                    addBytes(r, bos);
                    break;
                default:
                    throw new IllegalStateException("Random is broken");
                }
            }
            doTest(bos.toByteArray());
        }
    }

    private void addLong(Random r, long prev, ByteArrayOutputStream bos) {
        long theValue = prev;
        if (r.nextInt(10) != 0) {
            theValue = r.nextLong();
        }
        bos.write((byte) (theValue >>> 56));
        bos.write((byte) (theValue >>> 48));
        bos.write((byte) (theValue >>> 40));
        bos.write((byte) (theValue >>> 32));
        bos.write((byte) (theValue >>> 24));
        bos.write((byte) (theValue >>> 16));
        bos.write((byte) (theValue >>> 8));
        bos.write((byte) theValue);
    }

    private void addInt(Random r, int prev, ByteArrayOutputStream bos) {
        int theValue = prev;
        if (r.nextInt(10) != 0) {
            theValue = r.nextInt();
        }
        bos.write((byte) (theValue >>> 24));
        bos.write((byte) (theValue >>> 16));
        bos.write((byte) (theValue >>> 8));
        bos.write((byte) theValue);
    }

    private void addString(LineFileDocs lineFileDocs, ByteArrayOutputStream bos) throws IOException {
        String s = lineFileDocs.nextDoc().get("body");
        bos.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private void addBytes(Random r, ByteArrayOutputStream bos) throws IOException {
        byte bytes[] = new byte[TestUtil.nextInt(r, 1, 10000)];
        r.nextBytes(bytes);
        bos.write(bytes);
    }

    public void testRepetitionsSThreads() throws Exception {
        final Random r = random();
        int threadCount = TestUtil.nextInt(r, 2, 6);
        Thread[] threads = new Thread[threadCount];
        final CountDownLatch startingGun = new CountDownLatch(1);
        for (int tid=0; tid < threadCount; tid++) {
            final long seed = r.nextLong();
            threads[tid] = new Thread() {
                @Override
                public void run() {
                    try {
                        Random r = new Random(seed);
                        startingGun.await();
                        for (int i = 0; i < 10; i++) {
                            int numShorts = TestUtil.nextInt(r, 1, 40000);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            short theValue = (short) r.nextInt(65535);
                            for (int j = 0; j < numShorts; j++) {
                                if (r.nextInt(10) == 0) {
                                    theValue = (short) r.nextInt(65535);
                                }
                                bos.write((byte) (theValue >>> 8));
                                bos.write((byte) theValue);
                            }
                            doTest(bos.toByteArray());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            threads[tid].start();
        }
        startingGun.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    private void doTest(byte bytes[]) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        StreamInput rawIn = new ByteBufferStreamInput(bb);
        Compressor c = compressor;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamStreamOutput rawOs = new OutputStreamStreamOutput(bos);
        StreamOutput os = c.streamOutput(rawOs);

        Random r = random();
        int bufferSize = r.nextBoolean() ? 65535 : TestUtil.nextInt(random(), 1, 70000);
        int prepadding = r.nextInt(70000);
        int postpadding = r.nextInt(70000);
        byte buffer[] = new byte[prepadding + bufferSize + postpadding];
        r.nextBytes(buffer); // fill block completely with junk
        int len;
        while ((len = rawIn.read(buffer, prepadding, bufferSize)) != -1) {
            os.write(buffer, prepadding, len);
        }
        os.close();
        rawIn.close();

        // now we have compressed byte array

        byte compressed[] = bos.toByteArray();
        ByteBuffer bb2 = ByteBuffer.wrap(compressed);
        StreamInput compressedIn = new ByteBufferStreamInput(bb2);
        StreamInput in = c.streamInput(compressedIn);

        // randomize constants again
        bufferSize = r.nextBoolean() ? 65535 : TestUtil.nextInt(random(), 1, 70000);
        prepadding = r.nextInt(70000);
        postpadding = r.nextInt(70000);
        buffer = new byte[prepadding + bufferSize + postpadding];
        r.nextBytes(buffer); // fill block completely with junk

        ByteArrayOutputStream uncompressedOut = new ByteArrayOutputStream();
        while ((len = in.read(buffer, prepadding, bufferSize)) != -1) {
            uncompressedOut.write(buffer, prepadding, len);
        }
        uncompressedOut.close();

        assertArrayEquals(bytes, uncompressedOut.toByteArray());
    }
}
