package hkube.utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class PrintStreamInterceptor extends PrintStream {
    IPrinter printer;
    public PrintStreamInterceptor(OutputStream out, IPrinter printer)
    {
        super(out, true);
        this.printer = printer;
    }
    @Override
    public void print(String s)
    {   printer.print(s);
        super.print(s);
    }
}

