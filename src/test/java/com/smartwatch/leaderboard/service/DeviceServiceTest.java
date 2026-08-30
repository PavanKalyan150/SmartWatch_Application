package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.request.DeviceCapabilityRequest;
import com.smartwatch.leaderboard.dto.request.DeviceRequest;
import com.smartwatch.leaderboard.dto.response.DeviceCapabilityResponse;
import com.smartwatch.leaderboard.dto.response.DeviceResponse;
import com.smartwatch.leaderboard.model.Device;
import com.smartwatch.leaderboard.model.DeviceCapability;
import com.smartwatch.leaderboard.repository.DeviceCapabilityRepository;
import com.smartwatch.leaderboard.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceCapabilityRepository deviceCapabilityRepository;

    @InjectMocks private DeviceService deviceService;

    private static final Long DEVICE_ID = 1L;
    private static final String DEVICE_NAME = "Galaxy Watch 6";
    private static final String MANUFACTURER = "Samsung";
    private static final String MODEL = "SM-R940";
    private static final String CAPABILITY_CODE = "HEART_RATE";

    private Device device;
    private DeviceRequest deviceRequest;
    private DeviceCapabilityRequest capabilityRequest;

    @BeforeEach
    void setUp() {
        device = Device.builder()
                .id(DEVICE_ID)
                .deviceName(DEVICE_NAME)
                .manufacturer(MANUFACTURER)
                .model(MODEL)
                .build();

        deviceRequest = new DeviceRequest();
        deviceRequest.setDeviceName(DEVICE_NAME);
        deviceRequest.setManufacturer(MANUFACTURER);
        deviceRequest.setModel(MODEL);

        capabilityRequest = new DeviceCapabilityRequest();
        capabilityRequest.setCapabilityCode(CAPABILITY_CODE);
    }

    // ---------- createDevice ----------

    @Nested
    class CreateDevice {

        @Test
        void shouldCreateDeviceWhenNoDuplicateExists() {
            when(deviceRepository.existsByDeviceNameAndManufacturerAndModel(
                    DEVICE_NAME, MANUFACTURER, MODEL)).thenReturn(false);

            DeviceResponse response = deviceService.createDevice(deviceRequest);

            ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(captor.capture());
            Device saved = captor.getValue();

            assertThat(saved.getDeviceName()).isEqualTo(DEVICE_NAME);
            assertThat(saved.getManufacturer()).isEqualTo(MANUFACTURER);
            assertThat(saved.getModel()).isEqualTo(MODEL);

            assertThat(response.getDeviceName()).isEqualTo(DEVICE_NAME);
            assertThat(response.getManufacturer()).isEqualTo(MANUFACTURER);
            assertThat(response.getModel()).isEqualTo(MODEL);
        }

        @Test
        void shouldThrowWhenDuplicateDeviceExists() {
            when(deviceRepository.existsByDeviceNameAndManufacturerAndModel(
                    DEVICE_NAME, MANUFACTURER, MODEL)).thenReturn(true);

            assertThatThrownBy(() -> deviceService.createDevice(deviceRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(deviceRepository, never()).save(any());
        }
    }

    // ---------- updateDevice ----------

    @Nested
    class UpdateDevice {

        @Test
        void shouldUpdateDeviceFieldsWhenFound() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
            deviceRequest.setDeviceName("Galaxy Watch 7");
            deviceRequest.setModel("SM-R950");

            DeviceResponse response = deviceService.updateDevice(DEVICE_ID, deviceRequest);

            verify(deviceRepository).save(device);
            assertThat(device.getDeviceName()).isEqualTo("Galaxy Watch 7");
            assertThat(device.getModel()).isEqualTo("SM-R950");
            assertThat(response.getDeviceName()).isEqualTo("Galaxy Watch 7");
            assertThat(response.getModel()).isEqualTo("SM-R950");
        }

        @Test
        void shouldThrowWhenUpdatingMissingDevice() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.updateDevice(DEVICE_ID, deviceRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceRepository, never()).save(any());
        }
    }

    // ---------- getAllDevices ----------

    @Nested
    class GetAllDevices {

        @Test
        void shouldReturnMappedListOfDevices() {
            Device second = Device.builder()
                    .id(2L).deviceName("Pixel Watch 2")
                    .manufacturer("Google").model("GW2").build();
            when(deviceRepository.findAll()).thenReturn(List.of(device, second));

            List<DeviceResponse> responses = deviceService.getAllDevices();

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(DeviceResponse::getDeviceName)
                    .containsExactly(DEVICE_NAME, "Pixel Watch 2");
            assertThat(responses).extracting(DeviceResponse::getManufacturer)
                    .containsExactly(MANUFACTURER, "Google");
        }

        @Test
        void shouldReturnEmptyListWhenNoDevicesExist() {
            when(deviceRepository.findAll()).thenReturn(Collections.emptyList());

            List<DeviceResponse> responses = deviceService.getAllDevices();

            assertThat(responses).isEmpty();
        }
    }

    // ---------- addCapability ----------

    @Nested
    class AddCapability {

        @Test
        void shouldAddCapabilityWhenDeviceExistsAndCapabilityIsNew() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
            when(deviceCapabilityRepository
                    .existsByDeviceIdAndCapabilityCode(DEVICE_ID, CAPABILITY_CODE))
                    .thenReturn(false);

            DeviceCapabilityResponse response =
                    deviceService.addCapability(DEVICE_ID, capabilityRequest);

            ArgumentCaptor<DeviceCapability> captor =
                    ArgumentCaptor.forClass(DeviceCapability.class);
            verify(deviceCapabilityRepository).save(captor.capture());
            DeviceCapability saved = captor.getValue();

            assertThat(saved.getDevice()).isSameAs(device);
            assertThat(saved.getCapabilityCode()).isEqualTo(CAPABILITY_CODE);
            assertThat(response.getCapabilityCode()).isEqualTo(CAPABILITY_CODE);
            assertThat(response.getDeviceId()).isEqualTo(DEVICE_ID);
        }

        @Test
        void shouldThrowWhenDeviceNotFound() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.addCapability(DEVICE_ID, capabilityRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceCapabilityRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenCapabilityAlreadyExistsForDevice() {
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
            when(deviceCapabilityRepository
                    .existsByDeviceIdAndCapabilityCode(DEVICE_ID, CAPABILITY_CODE))
                    .thenReturn(true);

            assertThatThrownBy(() -> deviceService.addCapability(DEVICE_ID, capabilityRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Capability already exists")
                    .hasMessageContaining(CAPABILITY_CODE);

            verify(deviceCapabilityRepository, never()).save(any());
        }
    }

    // ---------- removeCapability ----------

    @Nested
    class RemoveCapability {

        @Test
        void shouldDeleteCapabilityWhenDeviceAndCapabilityExist() {
            DeviceCapability capability = DeviceCapability.builder()
                    .id(99L).device(device).capabilityCode(CAPABILITY_CODE).build();
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
            when(deviceCapabilityRepository
                    .findByDeviceIdAndCapabilityCode(DEVICE_ID, CAPABILITY_CODE))
                    .thenReturn(Optional.of(capability));

            deviceService.removeCapability(DEVICE_ID, CAPABILITY_CODE);

            verify(deviceCapabilityRepository).delete(capability);
        }

        @Test
        void shouldThrowWhenDeviceNotFound() {
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(false);

            assertThatThrownBy(() ->
                    deviceService.removeCapability(DEVICE_ID, CAPABILITY_CODE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceCapabilityRepository, never()).delete(any(DeviceCapability.class));
            verify(deviceCapabilityRepository, never())
                    .findByDeviceIdAndCapabilityCode(any(), any());
        }

        @Test
        void shouldThrowWhenCapabilityNotFoundOnDevice() {
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
            when(deviceCapabilityRepository
                    .findByDeviceIdAndCapabilityCode(DEVICE_ID, CAPABILITY_CODE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    deviceService.removeCapability(DEVICE_ID, CAPABILITY_CODE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Capability not found")
                    .hasMessageContaining(CAPABILITY_CODE);

            verify(deviceCapabilityRepository, never()).delete(any(DeviceCapability.class));
        }
    }

    // ---------- getCapabilities ----------

    @Nested
    class GetCapabilities {

        @Test
        void shouldReturnMappedCapabilitiesWhenDeviceExists() {
            DeviceCapability cap1 = DeviceCapability.builder()
                    .id(1L).device(device).capabilityCode("HEART_RATE").build();
            DeviceCapability cap2 = DeviceCapability.builder()
                    .id(2L).device(device).capabilityCode("STEPS").build();

            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
            when(deviceCapabilityRepository.findByDeviceId(DEVICE_ID))
                    .thenReturn(List.of(cap1, cap2));

            List<DeviceCapabilityResponse> responses =
                    deviceService.getCapabilities(DEVICE_ID);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(DeviceCapabilityResponse::getCapabilityCode)
                    .containsExactly("HEART_RATE", "STEPS");
            assertThat(responses).allSatisfy(r ->
                    assertThat(r.getDeviceId()).isEqualTo(DEVICE_ID));
        }

        @Test
        void shouldReturnEmptyListWhenDeviceHasNoCapabilities() {
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
            when(deviceCapabilityRepository.findByDeviceId(DEVICE_ID))
                    .thenReturn(Collections.emptyList());

            List<DeviceCapabilityResponse> responses =
                    deviceService.getCapabilities(DEVICE_ID);

            assertThat(responses).isEmpty();
        }

        @Test
        void shouldThrowWhenDeviceNotFound() {
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(false);

            assertThatThrownBy(() -> deviceService.getCapabilities(DEVICE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceCapabilityRepository, never()).findByDeviceId(any());
        }
    }
}