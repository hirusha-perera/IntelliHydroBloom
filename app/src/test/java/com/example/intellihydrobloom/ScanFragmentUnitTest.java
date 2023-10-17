package com.example.intellihydrobloom;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.ejml.simple.SimpleMatrix;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScanFragmentUnitTest {

    @InjectMocks
    private ScanFragment scanFragment;

    @Mock
    private SimpleMatrix mockMatrix;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRelu() {
        double[][] testData = {
                {-5.0, 2.0},
                {-1.0, 3.0}
        };
        SimpleMatrix matrix = new SimpleMatrix(testData);

        SimpleMatrix result = ScanFragment.relu(matrix);

        assertEquals(0.0, result.get(0, 0), 0.001);
        assertEquals(2.0, result.get(0, 1), 0.001);
        assertEquals(0.0, result.get(1, 0), 0.001);
        assertEquals(3.0, result.get(1, 1), 0.001);
    }

    @Test
    public void testMaxPooling() {
        when(mockMatrix.numRows()).thenReturn(4);
        when(mockMatrix.numCols()).thenReturn(4);
        double[][] sampleData = {
                {1.0, 2.0, 3.0, 4.0},
                {5.0, 6.0, 7.0, 8.0},
                {9.0, 10.0, 11.0, 12.0},
                {13.0, 14.0, 15.0, 16.0}
        };
        SimpleMatrix matrix = new SimpleMatrix(sampleData);

        SimpleMatrix result = ScanFragment.maxPooling(matrix, 2, 2);

        assertEquals(6.0, result.get(0, 0), 0.001);
        assertEquals(8.0, result.get(0, 1), 0.001);
        assertEquals(14.0, result.get(1, 0), 0.001);
        assertEquals(16.0, result.get(1, 1), 0.001);
    }

    @Test
    public void testReshape() {
        double[][] testData = {
                {1.0, 2.0},
                {3.0, 4.0},
                {5.0, 6.0}
        };
        SimpleMatrix matrix = new SimpleMatrix(testData);

        SimpleMatrix result = ScanFragment.reshape(matrix, 2, 3);

        assertEquals(1.0, result.get(0, 0), 0.001);
        assertEquals(2.0, result.get(0, 1), 0.001);
        assertEquals(3.0, result.get(0, 2), 0.001);
        assertEquals(4.0, result.get(1, 0), 0.001);
        assertEquals(5.0, result.get(1, 1), 0.001);
        assertEquals(6.0, result.get(1, 2), 0.001);
    }

    @Test
    public void testFlattenMatrix() {
        double[][] testData = {
                {1.0, 2.0},
                {3.0, 4.0}
        };
        SimpleMatrix matrix = new SimpleMatrix(testData);

        SimpleMatrix result = scanFragment.flattenMatrix(matrix);

        assertEquals(1.0, result.get(0, 0), 0.001);
        assertEquals(2.0, result.get(1, 0), 0.001);
        assertEquals(3.0, result.get(2, 0), 0.001);
        assertEquals(4.0, result.get(3, 0), 0.001);
    }


}
