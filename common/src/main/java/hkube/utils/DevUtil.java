package hkube.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;


public class DevUtil {

    public void printMemoryUsage(String title) {

        System.out.print("\n\n"+title+"\n");
        System.out.print("tota lheap " + Runtime.getRuntime().totalMemory()/(1024*1024)+ "\n");
        System.out.print("free heap " + Runtime.getRuntime().freeMemory()/(1024*1024)+ "\n");
        System.out.print("used heap " +(  Runtime.getRuntime().totalMemory() -Runtime.getRuntime().freeMemory())/(1024*1024)+ "\n");
//        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
////            if (mpBean.getType() == MemoryType.HEAP) {
//            System.out.printf(
//                    "Name: %s: %s\n",
//                    mpBean.getName(), mpBean.getUsage()
//            );
////            }
//        }
        try {
            String pid = ManagementFactory.getRuntimeMXBean().getName();
            pid = pid.substring(0, pid.indexOf("@"));
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    "ps ux|grep " + pid + "|awk '{print $5}'"
            };
            Process pr = Runtime.getRuntime().exec(cmd);


            BufferedReader input = new BufferedReader(new InputStreamReader(
                    pr.getInputStream()));

            String line = null;

            line = input.readLine();
            {
                int value = Integer.valueOf(line).intValue() / 1024;
                System.out.println("VSZ " + value);
            }
            cmd = new String[]{
                    "/bin/sh",
                    "-c",
                    "ps ux|grep " + pid + "|awk '{print $6}'"
            };
            pr = Runtime.getRuntime().exec(cmd);


            input = new BufferedReader(new InputStreamReader(
                    pr.getInputStream()));

            line = input.readLine();
            {
                int value = Integer.valueOf(line).intValue() / 1024;
                System.out.println("RSS " + value);
            }


            System.out.print("\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
