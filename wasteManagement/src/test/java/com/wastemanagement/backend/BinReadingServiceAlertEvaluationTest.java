package com.wastemanagement.backend;

import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinReadingMapper;
import com.wastemanagement.backend.model.collection.AlertType;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.service.alert.AlertService;
import com.wastemanagement.backend.service.collection.BinReadingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BinReading Service - Alert Evaluation Tests")
class BinReadingServiceAlertEvaluationTest {

    @Mock
    private BinReadingRepository binReadingRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private BinReadingServiceImpl binReadingService;

    private BinReadingRequestDTO requestDTO;
    private BinReading savedReading;

    @BeforeEach
    void setup() {
        requestDTO = new BinReadingRequestDTO();
        requestDTO.setBinId("bin-001");
        requestDTO.setTs(new Date());
        requestDTO.setFillPct(50.0);
        requestDTO.setBatteryPct(80);
        requestDTO.setTemperatureC(25.0);
        requestDTO.setSignalDbm(-70);

        savedReading = BinReadingMapper.toEntity(requestDTO);
        savedReading.setId("reading-001");
    }

    @Test
    @DisplayName("Should trigger LEVEL_CRITICAL alert when fillPct >= 95")
    void testLevelCriticalAlert() {
        // Arrange
        requestDTO.setFillPct(95.5);
        savedReading.setFillPct(95.5);
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(AlertType.LEVEL_CRITICAL, capturedAlert.getType());
        assertEquals(95.5, capturedAlert.getValue());
        assertEquals("bin-001", capturedAlert.getBinId());
        assertFalse(capturedAlert.isCleared());
    }

    @Test
    @DisplayName("Should trigger LEVEL_HIGH alert when fillPct >= 80 and < 95")
    void testLevelHighAlert() {
        // Arrange
        requestDTO.setFillPct(85.0);
        savedReading.setFillPct(85.0);
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(AlertType.LEVEL_HIGH, capturedAlert.getType());
        assertEquals(85.0, capturedAlert.getValue());
    }

    @Test
    @DisplayName("Should trigger BATTERY_LOW alert when batteryPct <= 20")
    void testBatteryLowAlert() {
        // Arrange
        requestDTO.setBatteryPct(15);
        savedReading.setBatteryPct(15);
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(AlertType.BATTERY_LOW, capturedAlert.getType());
        assertEquals(15, capturedAlert.getValue());
    }

    @Test
    @DisplayName("Should trigger SENSOR_ANOMALY alert when temperatureC > 60")
    void testSensorAnomalyAlert() {
        // Arrange
        requestDTO.setTemperatureC(65.5);
        savedReading.setTemperatureC(65.5);
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(AlertType.SENSOR_ANOMALY, capturedAlert.getType());
        assertEquals(65.5, capturedAlert.getValue());
    }

    @Test
    @DisplayName("Should trigger multiple alerts when multiple conditions are met")
    void testMultipleAlertsTriggered() {
        // Arrange: Critical fill level + Low battery + High temperature
        requestDTO.setFillPct(96.0);
        requestDTO.setBatteryPct(10);
        requestDTO.setTemperatureC(70.0);
        
        savedReading.setFillPct(96.0);
        savedReading.setBatteryPct(10);
        savedReading.setTemperatureC(70.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        assertNotNull(result);
        // Should create 3 alerts: LEVEL_CRITICAL + BATTERY_LOW + SENSOR_ANOMALY
        verify(alertService, times(3)).createAlert(any(AlertRequestDTO.class));
    }

    @Test
    @DisplayName("Should NOT trigger any alert when all values are within normal range")
    void testNoAlertsTriggered() {
        // Arrange: All values within normal range
        requestDTO.setFillPct(50.0);
        requestDTO.setBatteryPct(80);
        requestDTO.setTemperatureC(25.0);
        
        savedReading.setFillPct(50.0);
        savedReading.setBatteryPct(80);
        savedReading.setTemperatureC(25.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        assertNotNull(result);
        verify(alertService, never()).createAlert(any());
    }

    @Test
    @DisplayName("Should handle alert creation error gracefully")
    void testAlertCreationErrorDoesNotFailReading() {
        // Arrange: Mock alert service to throw exception
        requestDTO.setFillPct(95.0);
        savedReading.setFillPct(95.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);
        doThrow(new RuntimeException("Alert service error")).when(alertService).createAlert(any());

        // Act & Assert: Should not throw exception
        assertDoesNotThrow(() -> {
            BinReadingResponseDTO result = binReadingService.create(requestDTO);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("Should set alert timestamp to reading timestamp")
    void testAlertTimestampMatchesReading() {
        // Arrange
        Date readingTime = new Date(1234567890000L);
        requestDTO.setTs(readingTime);
        requestDTO.setFillPct(90.0);
        
        savedReading.setTs(readingTime);
        savedReading.setFillPct(90.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(readingTime, capturedAlert.getTs());
    }

    @Test
    @DisplayName("Should set alert binId to reading binId")
    void testAlertBinIdMatchesReading() {
        // Arrange
        String binId = "bin-special-123";
        requestDTO.setBinId(binId);
        requestDTO.setFillPct(88.0);
        
        savedReading.setBinId(binId);
        savedReading.setFillPct(88.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(binId, capturedAlert.getBinId());
    }

    @Test
    @DisplayName("Should set alert cleared flag to false")
    void testAlertClearedFlagIsFalse() {
        // Arrange
        requestDTO.setFillPct(82.0);
        savedReading.setFillPct(82.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertFalse(capturedAlert.isCleared());
    }

    @Test
    @DisplayName("Should trigger LEVEL_HIGH for exactly 80% fill")
    void testBoundaryLevelHigh() {
        // Arrange
        requestDTO.setFillPct(80.0);
        savedReading.setFillPct(80.0);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(AlertType.LEVEL_HIGH, capturedAlert.getType());
    }

    @Test
    @DisplayName("Should trigger BATTERY_LOW for exactly 20% battery")
    void testBoundaryBatteryLow() {
        // Arrange
        requestDTO.setBatteryPct(20);
        savedReading.setBatteryPct(20);
        
        when(binReadingRepository.save(any())).thenReturn(savedReading);

        // Act
        BinReadingResponseDTO result = binReadingService.create(requestDTO);

        // Assert
        ArgumentCaptor<AlertRequestDTO> alertCaptor = ArgumentCaptor.forClass(AlertRequestDTO.class);
        verify(alertService).createAlert(alertCaptor.capture());
        
        AlertRequestDTO capturedAlert = alertCaptor.getValue();
        assertEquals(AlertType.BATTERY_LOW, capturedAlert.getType());
    }
}
