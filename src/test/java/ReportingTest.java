import org.junit.Test;

/*
 * Usually I would have written actual tests; in this case, I used it solely as
 * a quick way to execute the code to keep things under an hour
 */
public class ReportingTest {

    @Test
    public void getMonthlyBreakdown() {
        Reporting reporting = new Reporting();
        reporting.getMonthlyReferralsBreakdown("/deals.json");
    }
}
