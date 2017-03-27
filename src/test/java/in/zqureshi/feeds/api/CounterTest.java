package in.zqureshi.feeds.api;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CounterTest {
    @Test
    public void equality() {
        assertThat(new Counter("/foo", 1l))
            .isEqualTo(new Counter("/foo", 1l));

        assertThat(new Counter("/bar", 1l))
            .isNotEqualTo(new Counter("/bar", 100l));
    }

    @Test
    public void ordering() {
        Counter c1 = new Counter("/foo", 1L);
        Counter c2 = new Counter("/foo", 2L);

        assertThat(c1.compareTo(c2)).isLessThan(0);
        assertThat(c2.compareTo(c1)).isGreaterThan(0);
        assertThat(c1.compareTo(c1)).isZero();
    }
}