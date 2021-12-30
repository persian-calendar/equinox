package io.github.persiancalendar;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class MainTests {
    @Test
    public final void test_equinox_time() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"));
        int[][] elements = new int[][]{
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1401.pdf
                {2022, 3, 20, 19, 3/*should be 19*/, 3/* should be 26*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1400.pdf
                {2021, 3, 20, 13, 7/*should be 19*/, 14/* should be 28*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1399.pdf
                {2020, 3, 20, 7, 20/*should be 19*/, 43/* should be 37*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1398.pdf
                {2019, 3, 21, 1, 28, 13/*should be 27*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1397.pdf
                {2018, 3, 20, 19, 45, 53/*should be 28*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1396.pdf
                {2017, 3, 20, 13, 59/*should be 58*/, 38/*should be 40*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1395.pdf
                {2016, 3, 20, 8, 0, 55/*should be 12*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1394.pdf
                {2015, 3, 21, 2, 16/*should be 15*/, 0/*should be 11*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1393.pdf
                {2014, 3, 20, 20, 27, 41/*should be 7*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1392.pdf
                {2013, 3, 20, 14, 32/*should be 31*/, 41/*should be 56*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1391.pdf
                {2012, 3, 20, 8, 44, 19/*should be 27*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1390.pdf
                {2011, 3, 21, 2, 51/*should be 50*/, 38/*should be 25*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1389.pdf
                {2010, 3, 20, 21, 2, 49/*should be 13*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1388.pdf
                {2009, 3, 20, 15, 14/*should be 13*/, 50/*should be 39*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1387.pdf
                {2008, 3, 20, 9, 18, 17/*should be 19*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1386.pdf
                {2007, 3, 21, 3, 37, 16/*should be 26*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1385.pdf
                {2006, 3, 20, 21, 55, 19/*should be 35*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1384.pdf
                {2005, 3, 20, 16, 4/*should be 3*/, 37/*should be 24*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1383.pdf
                {2004, 3, 20, 10, 19/*should be 18*/, 41/*should be 37*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1382.pdf
                {2003, 3, 21, 4, 30/*should be 29*/, 22/*should be 45*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1381.pdf
                {2002, 3, 20, 22, 46, 12/*should be 2*/}
        };

        for (int[] item : elements) {
            calendar.setTime(Equinox.northwardEquinox(item[0]));
            Assert.assertEquals(String.valueOf(item[0]), item[0], calendar.get(Calendar.YEAR));
            Assert.assertEquals(String.valueOf(item[0]), item[1], calendar.get(Calendar.MONTH) + 1);
            Assert.assertEquals(String.valueOf(item[0]), item[2], calendar.get(Calendar.DAY_OF_MONTH));
            Assert.assertEquals(String.valueOf(item[0]), item[3], calendar.get(Calendar.HOUR_OF_DAY));
            Assert.assertEquals(String.valueOf(item[0]), item[4], calendar.get(Calendar.MINUTE));
            Assert.assertEquals(String.valueOf(item[0]), item[5], calendar.get(Calendar.SECOND));
        }

        // And not having random crashes
        for (int i = -2000; i <= 10000; i++) {
            Equinox.northwardEquinox(i);
        }
    }
}
