package io.github.persiancalendar;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class MainTests {
    @Test
    public final void test_equinox_time() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"));
        int[][] elements = new int[][]{
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1401.pdf
                {2022, 3, 20, 19, 3/*should be 19*/, 2/* should be 26*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1400.pdf
                {2021, 3, 20, 13, 7/*should be 19*/, 14/* should be 28*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1399.pdf
                {2020, 3, 20, 7, 19/*should be 19*/, 43/* should be 37*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1398.pdf
                {2019, 3, 21, 1, 28, 13/*should be 27*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1397.pdf
                {2018, 3, 20, 19, 44/*should be 45*/, 52/*should be 28*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1396.pdf
                {2017, 3, 20, 13, 58, 38/*should be 40*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1395.pdf
                {2016, 3, 20, 7/*should be 8*/, 59/*should be 0*/, 54/*should be 12*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1394.pdf
                {2015, 3, 21, 2, 14/*should be 15*/, 59/*should be 11*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1393.pdf
                {2014, 3, 20, 20, 26, 41/*should be 7*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1392.pdf
                {2013, 3, 20, 14, 31, 41/*should be 56*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1391.pdf
                {2012, 3, 20, 8, 44, 18/*should be 27*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1390.pdf
                {2011, 3, 21, 2, 50, 37/*should be 25*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1389.pdf
                {2010, 3, 20, 21, 1/*should be 2*/, 49/*should be 13*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1388.pdf
                {2009, 3, 20, 15, 13, 50/*should be 39*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1387.pdf
                {2008, 3, 20, 9, 18, 17/*should be 19*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1386.pdf
                {2007, 3, 21, 3, 37, 15/*should be 26*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1385.pdf
                {2006, 3, 20, 21, 55, 18/*should be 35*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1384.pdf
                {2005, 3, 20, 16, 3, 37/*should be 24*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1383.pdf
                {2004, 3, 20, 10, 18, 41/*should be 37*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1382.pdf
                {2003, 3, 21, 4, 30/*should be 29*/, 21/*should be 45*/},
                // https://calendar.ut.ac.ir/Fa/Tyear/Data/full-1381.pdf
                {2002, 3, 20, 22, 46, 12/*should be 2*/}
        };

        for (int[] item : elements) {
            calendar.setTime(Equinox.northwardEquinox(item[0]));
            assertEquals(String.valueOf(item[0]), item[0], calendar.get(Calendar.YEAR));
            assertEquals(String.valueOf(item[0]), item[1], calendar.get(Calendar.MONTH) + 1);
            assertEquals(String.valueOf(item[0]), item[2], calendar.get(Calendar.DAY_OF_MONTH));
            assertEquals(String.valueOf(item[0]), item[3], calendar.get(Calendar.HOUR_OF_DAY));
            assertEquals(String.valueOf(item[0]), item[4], calendar.get(Calendar.MINUTE));
            assertEquals(String.valueOf(item[0]), item[5], calendar.get(Calendar.SECOND));
        }

        // And not having random crashes
        for (int i = -2000; i <= 10000; i++) {
            Equinox.northwardEquinox(i);
        }

        assertEquals(1584676183400L, Equinox.northwardEquinox(2020).getTime());
        assertEquals(1592689390621L, Equinox.northernSolstice(2020).getTime());
        assertEquals(1600781435095L, Equinox.southwardEquinox(2020).getTime());
        assertEquals(1608544954756L, Equinox.southernSolstice(2020).getTime());
    }
}
