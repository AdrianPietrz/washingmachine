package edu.iis.mto.testreactor.washingmachine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class WashingMachineTest {

    @Mock
    private DirtDetector dirtDetector;
    @Mock
    private Engine engine;
    @Mock
    private WaterPump waterPump;
    private WashingMachine washingMashine;
    private LaundryBatch laundryBatchBasic;
    ProgramConfiguration programConfigurationBasic;

    @BeforeEach
    void setUp() throws Exception {
        washingMashine = new WashingMachine(dirtDetector, engine, waterPump);
        laundryBatchBasic = LaundryBatch.builder().withMaterialType(Material.SYNTETIC).withWeightKg(2).build();
        programConfigurationBasic = ProgramConfiguration.builder().withProgram(Program.MEDIUM).withSpin(true).build();
    }

    @Test
    void methodShouldReturnErrorCodeTooHeavy() {

        LaundryBatch laundryBatchWool = LaundryBatch.builder().withMaterialType(Material.WOOL).withWeightKg(5).build();
        LaundryBatch laundryBatchOther = LaundryBatch.builder().withMaterialType(Material.SYNTETIC).withWeightKg(10).build();

        assertEquals(ErrorCode.TOO_HEAVY, washingMashine.start(laundryBatchWool, null).getErrorCode());
        assertEquals(ErrorCode.TOO_HEAVY, washingMashine.start(laundryBatchOther, null).getErrorCode());
    }

    @Test
    void methodShouldNotReturnErrorCodeTooHeavy(){
        LaundryBatch laundryBatchWool = LaundryBatch.builder().withMaterialType(Material.WOOL).withWeightKg(2).build();
        LaundryBatch laundryBatchOther = LaundryBatch.builder().withMaterialType(Material.SYNTETIC).withWeightKg(4).build();

        assertNotEquals(ErrorCode.TOO_HEAVY, washingMashine.start(laundryBatchWool, null).getErrorCode());
        assertNotEquals(ErrorCode.TOO_HEAVY, washingMashine.start(laundryBatchOther, null).getErrorCode());
    }

    @Test
    void methodShouldReturnSuccess(){
        LaundryBatch laundryBatchWool = LaundryBatch.builder().withMaterialType(Material.WOOL).withWeightKg(2).build();
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder().withProgram(Program.MEDIUM).withSpin(true).build();

        assertEquals(ErrorCode.NO_ERROR, washingMashine.start(laundryBatchWool,programConfiguration).getErrorCode());
    }

    @Test
    void methodShouldThrowEngineException() throws EngineException {
        LaundryBatch laundryBatchWool = LaundryBatch.builder().withMaterialType(Material.SYNTETIC).withWeightKg(2).build();
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder().withProgram(Program.SHORT).withSpin(true).build();

        doThrow(EngineException.class).when(engine).runWashing(anyInt());

        assertEquals(ErrorCode.ENGINE_FAILURE, washingMashine.start(laundryBatchWool, programConfiguration).getErrorCode());
    }

    @Test
    void methodShouldReturnErrorCodeEngineFailureCausedBySpin() throws EngineException {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder().withProgram(Program.SHORT).withSpin(true).build();

        doThrow(EngineException.class).when(engine).spin();

        assertEquals(ErrorCode.ENGINE_FAILURE, washingMashine.start(laundryBatchBasic, programConfiguration).getErrorCode());
    }

    @Test
    void methodShouldReturnErrorCodeWaterPumpFailure() throws WaterPumpException {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder().withProgram(Program.SHORT).withSpin(true).build();

        doThrow(WaterPumpException.class).when(waterPump).pour(anyDouble());

        assertEquals(ErrorCode.WATER_PUMP_FAILURE, washingMashine.start(laundryBatchBasic, programConfiguration).getErrorCode());
    }


    @Test
    void methodShouldSetProgramToRunAsLongBasedOnDirtPercentage() {

        ProgramConfiguration programConfiguration = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).build();
        when(dirtDetector.detectDirtDegree(any())).thenReturn(new Percentage(90));

        assertEquals(Program.LONG, washingMashine.start(laundryBatchBasic, programConfiguration).getRunnedProgram());
    }

    @Test
    void methodShouldSetProgramToRunAsMediumBasedOnDirtPercentage() {

        ProgramConfiguration programConfiguration = ProgramConfiguration.builder().withProgram(Program.AUTODETECT).build();
        when(dirtDetector.detectDirtDegree(any())).thenReturn(new Percentage(50));

        assertEquals(Program.MEDIUM, washingMashine.start(laundryBatchBasic, programConfiguration).getRunnedProgram());
    }






    @Test
    void methodShouldInvokeEngineSpinOnce() throws EngineException {
        washingMashine.start(laundryBatchBasic, programConfigurationBasic);

        verify(engine,times(1)).spin();
    }

    @Test
    void methodShouldNotCallEngineSpin() throws EngineException {
        washingMashine.start(laundryBatchBasic, ProgramConfiguration.builder().withProgram(Program.AUTODETECT).withSpin(false).build());

        verify(engine,times(0)).spin();
    }

    @Test
    void dirtDetectorShouldNotBeCalled(){
        washingMashine.start(laundryBatchBasic, programConfigurationBasic);

        verify(dirtDetector,times(0)).detectDirtDegree(laundryBatchBasic);
    }

    @Test
    void allBasicWashingMethodsShouldBeCalled() throws EngineException, WaterPumpException {
        washingMashine.start(laundryBatchBasic,programConfigurationBasic);

        verify(waterPump, times(1)).pour(laundryBatchBasic.getWeightKg());
        verify(engine, times(1)).runWashing(programConfigurationBasic.getProgram().getTimeInMinutes());
        verify(waterPump, times(1)).release();
    }
}
