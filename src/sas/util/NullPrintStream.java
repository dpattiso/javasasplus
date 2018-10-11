package sas.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class NullPrintStream extends PrintStream
{

	public NullPrintStream()
	{
		super(new NullOutputStream());
	}
}