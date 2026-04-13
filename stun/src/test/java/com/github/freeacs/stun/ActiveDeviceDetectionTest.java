package com.github.freeacs.stun;

import com.github.freeacs.common.util.TimestampMap;
import com.github.freeacs.dbi.*;
import com.github.freeacs.dbi.util.SyslogClient;
import de.javawi.jstun.StunServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActiveDeviceDetectionTest {

    @Mock private DataSource dataSource;
    @Mock private DBI dbi;
    @Mock private ACS acs;
    @Mock private Syslog syslog;

    private ActiveDeviceDetection task;

    @BeforeEach
    void setUp() {
        // Use lenient to avoid UnnecessaryStubbingException if a mock isn't called in every test branch
        lenient().when(dbi.getAcs()).thenReturn(acs);
        lenient().when(dbi.getSyslog()).thenReturn(syslog);
        task = new ActiveDeviceDetection(dataSource, dbi, "ActiveDeviceTest");
    }

    @Test
    void testCleanupOfInactiveDevices() throws Throwable {
        // 1. Setup: Define the current time for the test
        long now = System.currentTimeMillis();

        // 2. Mock getThisLaunchTms() to return 'now'
        // We use spy() so we can mock a specific method while keeping the rest of the task real
        ActiveDeviceDetection spyTask = spy(task);
        doReturn(now).when(spyTask).getThisLaunchTms();


        // Setup: Populate the global STUN map with an entry older than 1 hour
        TimestampMap activeClients = StunServer.getActiveStunClients();
        activeClients.getMap().clear();

        String oldIp = "1.2.3.4:3478";
        long overOneHourAgo = System.currentTimeMillis() - (65 * 60 * 1000);
        activeClients.putSync(oldIp, overOneHourAgo);

        // Use mockConstruction to intercept "new ACSUnit(...)" calls and prevent DB connections
        try (MockedConstruction<ACSUnit> ignored = mockConstruction(ACSUnit.class)) {
            // Execute: Should identify and remove devices inactive for > 1h
            // Execute using the spy
            spyTask.runImpl();
        }

        // Verify: The device should be evicted from the internal map
        assertFalse(activeClients.getMap().containsKey(oldIp),
                "The inactive device should have been removed from the map.");
    }

    @Test
    void testLogActiveDevicesDoesNotOverProcess() throws Throwable {
        long now = System.currentTimeMillis();
        ActiveDeviceDetection spyTask = spy(task);
        doReturn(now).when(spyTask).getThisLaunchTms();

        TimestampMap activeClients = StunServer.getActiveStunClients();
        activeClients.getMap().clear();

        // 120 Recent devices (should stay)
        for (int i = 0; i < 120; i++) {
            activeClients.putSync("192.168.1." + i + ":3478", now);
        }

        // 120 Old devices (should be removed by logInactiveDevices)
        long overOneHourAgo = now - (65 * 60000 + 10);
        for (int i = 0; i < 120; i++) {
            activeClients.putSync("192.168.2." + i + ":3478", overOneHourAgo);
        }

        assertEquals(240, activeClients.size(), "Total devices before processing.");

        try (MockedStatic<SyslogClient> syslogMock = mockStatic(SyslogClient.class);
             MockedConstruction<ACSUnit> acsUnitMock = mockConstruction(ACSUnit.class, (mock, context) -> {
                 when(mock.getUnitByValue(anyString(), any(), any())).thenReturn(mock(Unit.class));
             })) {

            // Use the spyTask to ensure getThisLaunchTms() returns 'now'
            spyTask.runImpl();

            // Verify syslog was called for the active ones
            syslogMock.verify(() -> SyslogClient.info(any(), any(), anyInt(), any(), any()), atLeastOnce());
        }

        // After cleanup, only the 120 recent devices should remain
        assertEquals(120, activeClients.size(), "Only active devices should remain in the map.");
    }

}
