
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Reporting {

    // Initialization of this object is costly - only do it once and reuse
    private final static ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> referredBy = new HashMap<>();

    public void getMonthlyReferralsBreakdown(String filePath) {
        List<Deal> deals;
        try {
            deals = parseJsonFile(filePath);
        } catch (Exception e) {
            System.err.println("Unable to read file");
            e.printStackTrace();
            return;
        }

        print(extractMonthlyBreakdown(deals));
    }

    // Extracts a map capturing the number of closed deals per month by initial referrers given a list of deals
    // Format of results: (yyyy-MM, (initialReferrer, referralCount))
    private Map<String, Map<String, Integer>> extractMonthlyBreakdown(List<Deal> deals) {
        // Entries without referrals are of no interest in this exercise; discard them
        List<Deal> referredDeals = deals.stream()
                .filter(d -> d.getReferredBy() != null)
                .collect(Collectors.toList());

        // Build an initial map of referrals
        for (Deal deal : referredDeals) {
            referredBy.put(deal.getName(), deal.getReferredBy());
        }

        // Capture the count per initial referrer broken down by month
        Map<String, Map<String, Integer>> breakdown = new HashMap<>();
        for (Deal deal : referredDeals) {
            String month = deal.getCloseDateYearMonth();

            // Identify the initial referrer
            String initialReferrer = getInitialReferrer(deal.getReferredBy());

            // Update the referral count for the initial referrer for this deal's closing month
            Map<String, Integer> referralCount = breakdown.getOrDefault(month, new HashMap<>());
            referralCount.put(initialReferrer, referralCount.merge(initialReferrer, 1, Integer::sum));
            breakdown.put(month, referralCount);
        }

        return breakdown;
    }

    // Given a referrer, find the initial referrer encountered so far
    private String getInitialReferrer(String referrer) {
        String downstreamReferrer = referrer;
        if (referredBy.containsKey(downstreamReferrer)) {
            downstreamReferrer = getInitialReferrer(referredBy.get(downstreamReferrer));

            // Update each downstream referrer to its initial referrer as we encounter them to avoid having
            // to re-calculate for other referrers along the same referral path.
            referredBy.put(referrer, downstreamReferrer);
        }

        return downstreamReferrer;
    }

    // Read and parse JSON file
    private static List<Deal> parseJsonFile(String filePath) throws Exception {
        String jsonContent = IOUtils.toString(Reporting.class.getResourceAsStream(filePath), "UTF-8");
        return mapper.readValue(jsonContent, new TypeReference<List<Deal>>(){});
    }

    private static void print(Map<String, Map<String, Integer>> monthlyReferrals) {
        monthlyReferrals.forEach((month, referrals) -> {
            System.out.println(month + ": ");

            referrals.forEach((initialReferrer, count) -> {
                System.out.println("\t" + initialReferrer + ": " + count);
            });

            System.out.println();
        });
    }
}


// Helper class for deserializing JSON payload
class Deal {
    private String name;

    @JsonProperty("close_date")
    private LocalDate closeDate;

    @JsonProperty("referred_by")
    private String referredBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public String getCloseDateYearMonth() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return closeDate.format(formatter);
    }

    public void setCloseDate(String closeDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        this.closeDate = LocalDate.parse(closeDate, formatter);
    }

    public String getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(String referredBy) {
        this.referredBy = referredBy;
    }

    @Override
    public String toString() {
        return this.name + " " + this.referredBy + " " + this.closeDate.toString();
    }
}
