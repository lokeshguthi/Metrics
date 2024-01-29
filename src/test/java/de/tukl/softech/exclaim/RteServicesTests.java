package de.tukl.softech.exclaim;

import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.utils.RteServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RteServicesTests {

    @Autowired
    private RteServices rteServices;

    @Test
    public void testListServices() {
        List<RteServices.TestName> strings = rteServices.availableTestsRequest();
        System.out.println(strings);
    }

    @Test
    public void testTriggerTest() throws ExecutionException, InterruptedException {
        RteServices.TestName test = new RteServices.TestName("SE1WS16", "1", "1");
        Team team = new Team("0", "0");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Integer>> testIdFutures = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            testIdFutures.add(executor.submit(() -> rteServices.requestTest(test, team, null, true)));
        }
        HashSet<Integer> testIds = new HashSet<>();
        for (Future<Integer> f : testIdFutures) {
            testIds.add(f.get());
        }
        assertEquals("Test ids are not distinct", testIdFutures.size(), testIds.size());
    }
}
