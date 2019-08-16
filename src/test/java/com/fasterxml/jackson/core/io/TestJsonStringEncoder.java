package com.fasterxml.jackson.core.io;

import java.io.StringWriter;
import java.util.Random;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.util.BufferRecyclers;

public class TestJsonStringEncoder
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testQuoteAsString() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        char[] result = encoder.quoteAsCharArray("foobar");
        assertArrayEquals("foobar".toCharArray(), result);
        result = encoder.quoteAsCharArray("\"x\"");
        assertArrayEquals("\\\"x\\\"".toCharArray(), result);
    }

    public void testQuoteCharSequenceAsString() throws Exception
    {
        StringBuilder output = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        builder.append("foobar");
        BufferRecyclers.quoteAsJsonText(builder, output);
        assertEquals("foobar", output.toString());
        builder.setLength(0);
        output.setLength(0);
        builder.append("\"x\"");
        BufferRecyclers.quoteAsJsonText(builder, output);
        assertEquals("\\\"x\\\"", output.toString());
    }

    // For [JACKSON-853]
    public void testQuoteLongAsString() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 1111; ++i) {
            sb.append('"');
            sb2.append("\\\"");
        }
        String input = sb.toString();
        String exp = sb2.toString();
        char[] result = encoder.quoteAsCharArray(input);
        assertEquals(2*input.length(), result.length);
        assertEquals(exp, new String(result));
        
    }

    public void testQuoteLongCharSequenceAsString() throws Exception
    {
        StringBuilder output = new StringBuilder();
        StringBuilder input = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 1111; ++i) {
            input.append('"');
            sb2.append("\\\"");
        }
        String exp = sb2.toString();
        BufferRecyclers.quoteAsJsonText(input, output);
        assertEquals(2*input.length(), output.length());
        assertEquals(exp, output.toString());

    }

    public void testQuoteAsUTF8() throws Exception
    {
        // In this case, let's actually use existing JsonGenerator to produce expected values
        JsonFactory f = new JsonFactory();
        JsonStringEncoder encoder = new JsonStringEncoder();
        int[] lengths = new int[] {
            5, 19, 200, 7000, 21000, 37000
        };
        for (int length : lengths) {
            String str = generateRandom(length);
            StringWriter sw = new StringWriter(length*2);
            JsonGenerator jgen = f.createGenerator(ObjectWriteContext.empty(), sw);
            jgen.writeString(str);
            jgen.close();
            String encoded = sw.toString();
            // ok, except need to remove surrounding quotes
            encoded = encoded.substring(1, encoded.length() - 1);
            byte[] expected = encoded.getBytes("UTF-8");
            byte[] actual = encoder.quoteAsUTF8(str);
            assertArrayEquals(expected, actual);
        }
    }

    public void testEncodeAsUTF8() throws Exception
    {
        JsonStringEncoder encoder = new JsonStringEncoder();
        String[] strings = new String[] {
                "a", "foobar", "p\u00f6ll\u00f6", "\"foo\"",
                generateRandom(200),
                generateRandom(5000),
                generateRandom(39000)
        };
        for (String str : strings) {
            assertArrayEquals(str.getBytes("UTF-8"), encoder.encodeAsUTF8(str));
        }
    }

    public void testCtrlChars() throws Exception
    {
        char[] input = new char[] { 0, 1, 2, 3, 4 };
        char[] quoted = BufferRecyclers.quoteAsJsonText(new String(input));
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", new String(quoted));
    }

    // [JACKSON-884]
    public void testCharSequenceWithCtrlChars() throws Exception
    {
        char[] input = new char[] { 0, 1, 2, 3, 4 };
        StringBuilder builder = new StringBuilder();
        builder.append(input);
        StringBuilder output = new StringBuilder();
        BufferRecyclers.quoteAsJsonText(builder, output);
        assertEquals("\\u0000\\u0001\\u0002\\u0003\\u0004", output.toString());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    
    private String generateRandom(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        Random rnd = new Random(length);
        for (int i = 0; i < length; ++i) {
            // let's limit it not to include surrogate pairs:
            char ch = (char) rnd.nextInt(0xCFFF);
            sb.append(ch);
        }
        return sb.toString();
    }
}

