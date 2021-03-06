/*******************************************************************************
 * Copyright (c) 2015-2018 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.deeplearning4j.regressiontest;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.BaseDL4JTest;
import org.deeplearning4j.TestUtils;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.LayerVertex;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.conf.serde.legacyformat.LegacyLayerDeserializer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInitXavier;
import org.deeplearning4j.regressiontest.customlayer100a.CustomLayer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.activations.impl.*;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.regularization.WeightDecay;
import org.nd4j.resources.Resources;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.*;

@Slf4j
public class RegressionTest100a extends BaseDL4JTest {

    @Override
    public DataType getDataType(){
        return DataType.FLOAT;
    }

    @Test
    public void testCustomLayer() throws Exception {

        File f = Resources.asFile("regression_testing/100a/CustomLayerExample_100a.bin");

        try {
            MultiLayerNetwork.load(f, true);
            fail("Expected exception");
        } catch (Exception e){
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("NeuralNetConfiguration.registerLegacyCustomClassesForJSON"));
        }

        NeuralNetConfiguration.registerLegacyCustomClassesForJSON(CustomLayer.class);

        MultiLayerNetwork net = MultiLayerNetwork.load(f, true);

        DenseLayer l0 = (DenseLayer) net.getLayer(0).conf().getLayer();
        assertEquals(new ActivationTanH(), l0.getActivationFn());
        assertEquals(new WeightDecay(0.03, false), TestUtils.getWeightDecayReg(l0));
        assertEquals(new RmsProp(0.95), l0.getIUpdater());

        CustomLayer l1 = (CustomLayer) net.getLayer(1).conf().getLayer();
        assertEquals(new ActivationTanH(), l1.getActivationFn());
        assertEquals(new ActivationSigmoid(), l1.getSecondActivationFunction());
        assertEquals(new RmsProp(0.95), l1.getIUpdater());


        INDArray outExp;
        File f2 = Resources.asFile("regression_testing/100a/CustomLayerExample_Output_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f2))){
            outExp = Nd4j.read(dis);
        }

        INDArray in;
        File f3 = Resources.asFile("regression_testing/100a/CustomLayerExample_Input_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f3))){
            in = Nd4j.read(dis);
        }

        INDArray outAct = net.output(in);

        assertEquals(outExp, outAct);


        //Check graph
        f = Resources.asFile("regression_testing/100a/CustomLayerExample_Graph_100a.bin");

        //Deregister custom class:
        new LegacyLayerDeserializer().getLegacyNamesMap().remove("CustomLayer");

        try {
            ComputationGraph.load(f, true);
            fail("Expected exception");
        } catch (Exception e){
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("NeuralNetConfiguration.registerLegacyCustomClassesForJSON"));
        }

        NeuralNetConfiguration.registerLegacyCustomClassesForJSON(CustomLayer.class);

        ComputationGraph graph = ComputationGraph.load(f, true);

        f2 = Resources.asFile("regression_testing/100a/CustomLayerExample_Graph_Output_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f2))){
            outExp = Nd4j.read(dis);
        }

        outAct = graph.outputSingle(in);

        assertEquals(outExp, outAct);
    }


    @Test
    public void testGravesLSTM() throws Exception {

        File f = Resources.asFile("regression_testing/100a/GravesLSTMCharModelingExample_100a.bin");
        MultiLayerNetwork net = MultiLayerNetwork.load(f, true);

        GravesLSTM l0 = (GravesLSTM) net.getLayer(0).conf().getLayer();
        assertEquals(new ActivationTanH(), l0.getActivationFn());
        assertEquals(200, l0.getNOut());
        assertEquals(new WeightInitXavier(), l0.getWeightInitFn());
        assertEquals(new WeightDecay(0.001, false), TestUtils.getWeightDecayReg(l0));
        assertEquals(new RmsProp(0.1), l0.getIUpdater());

        GravesLSTM l1 = (GravesLSTM) net.getLayer(1).conf().getLayer();
        assertEquals(new ActivationTanH(), l1.getActivationFn());
        assertEquals(200, l1.getNOut());
        assertEquals(new WeightInitXavier(), l1.getWeightInitFn());
        assertEquals(new WeightDecay(0.001, false), TestUtils.getWeightDecayReg(l1));
        assertEquals(new RmsProp(0.1), l1.getIUpdater());

        RnnOutputLayer l2 = (RnnOutputLayer) net.getLayer(2).conf().getLayer();
        assertEquals(new ActivationSoftmax(), l2.getActivationFn());
        assertEquals(77, l2.getNOut());
        assertEquals(new WeightInitXavier(), l2.getWeightInitFn());
        assertEquals(new WeightDecay(0.001, false), TestUtils.getWeightDecayReg(l0));
        assertEquals(new RmsProp(0.1), l0.getIUpdater());

        assertEquals(BackpropType.TruncatedBPTT, net.getLayerWiseConfigurations().getBackpropType());
        assertEquals(50, net.getLayerWiseConfigurations().getTbpttBackLength());
        assertEquals(50, net.getLayerWiseConfigurations().getTbpttFwdLength());

        INDArray outExp;
        File f2 = Resources.asFile("regression_testing/100a/GravesLSTMCharModelingExample_Output_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f2))){
            outExp = Nd4j.read(dis);
        }

        INDArray in;
        File f3 = Resources.asFile("regression_testing/100a/GravesLSTMCharModelingExample_Input_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f3))){
            in = Nd4j.read(dis);
        }

        INDArray outAct = net.output(in);

        assertEquals(outExp, outAct);
    }

    @Test
    public void testVae() throws Exception {

        File f = Resources.asFile("regression_testing/100a/VaeMNISTAnomaly_100a.bin");
        MultiLayerNetwork net = MultiLayerNetwork.load(f, true);

        VariationalAutoencoder l0 = (VariationalAutoencoder) net.getLayer(0).conf().getLayer();
        assertEquals(new ActivationLReLU(), l0.getActivationFn());
        assertEquals(32, l0.getNOut());
        assertArrayEquals(new int[]{256, 256}, l0.getEncoderLayerSizes());
        assertArrayEquals(new int[]{256, 256}, l0.getDecoderLayerSizes());
                assertEquals(new WeightInitXavier(), l0.getWeightInitFn());
        assertEquals(new WeightDecay(1e-4, false), TestUtils.getWeightDecayReg(l0));
        assertEquals(new Adam(0.05), l0.getIUpdater());

        INDArray outExp;
        File f2 = Resources.asFile("regression_testing/100a/VaeMNISTAnomaly_Output_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f2))){
            outExp = Nd4j.read(dis);
        }

        INDArray in;
        File f3 = Resources.asFile("regression_testing/100a/VaeMNISTAnomaly_Input_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f3))){
            in = Nd4j.read(dis);
        }

        INDArray outAct = net.output(in);

        assertEquals(outExp, outAct);
    }


    @Test
    @Ignore("AB 2019/05/23 - Failing on linux-x86_64-cuda-9.2 - see issue #7657")
    public void testYoloHouseNumber() throws Exception {

        File f = Resources.asFile("regression_testing/100a/HouseNumberDetection_100a.bin");
        ComputationGraph net = ComputationGraph.load(f, true);

        int nBoxes = 5;
        int nClasses = 10;

        ConvolutionLayer cl = (ConvolutionLayer)((LayerVertex)net.getConfiguration().getVertices().get("convolution2d_9")).getLayerConf().getLayer();
        assertEquals(nBoxes * (5 + nClasses), cl.getNOut());
        assertEquals(new ActivationIdentity(), cl.getActivationFn());
        assertEquals(ConvolutionMode.Same, cl.getConvolutionMode());
        assertEquals(new WeightInitXavier(), cl.getWeightInitFn());
        assertArrayEquals(new int[]{1,1}, cl.getKernelSize());
        assertArrayEquals(new int[]{1,1}, cl.getKernelSize());

        INDArray outExp;
        File f2 = Resources.asFile("regression_testing/100a/HouseNumberDetection_Output_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f2))){
            outExp = Nd4j.read(dis);
        }

        INDArray in;
        File f3 = Resources.asFile("regression_testing/100a/HouseNumberDetection_Input_100a.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(f3))){
            in = Nd4j.read(dis);
        }

        //Minor bug in 1.0.0-beta and earlier: not adding epsilon value to forward pass for batch norm
        //Which means: the record output doesn't have this. To account for this, we'll manually set eps to 0.0 here
        //https://github.com/deeplearning4j/deeplearning4j/issues/5836#issuecomment-405526228
        for(Layer l : net.getLayers()){
            if(l.conf().getLayer() instanceof BatchNormalization){
                BatchNormalization bn = (BatchNormalization) l.conf().getLayer();
                bn.setEps(0.0);
            }
        }

        INDArray outAct = net.outputSingle(in).castTo(outExp.dataType());

        boolean eq = outExp.equalsWithEps(outAct, 1e-4);
        if(!eq){
            log.info("Expected: {}", outExp);
            log.info("Actual: {}", outAct);
        }
        assertTrue("Output not equal", eq);
    }


    @Test
    public void testUpsampling2d() throws Exception {

        File f = Resources.asFile("regression_testing/100a/upsampling/net.bin");
        MultiLayerNetwork net = MultiLayerNetwork.load(f, true);

        INDArray in;
        File fIn = Resources.asFile("regression_testing/100a/upsampling/in.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(fIn))){
            in = Nd4j.read(dis);
        }

        INDArray label;
        File fLabels = Resources.asFile("regression_testing/100a/upsampling/labels.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(fLabels))){
            label = Nd4j.read(dis);
        }

        INDArray outExp;
        File fOutExp = Resources.asFile("regression_testing/100a/upsampling/out.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(fOutExp))){
            outExp = Nd4j.read(dis);
        }

        INDArray gradExp;
        File fGradExp = Resources.asFile("regression_testing/100a/upsampling/gradient.bin");
        try(DataInputStream dis = new DataInputStream(new FileInputStream(fGradExp))){
            gradExp = Nd4j.read(dis);
        }

        INDArray out = net.output(in, false);
        assertEquals(outExp, out);

        net.setInput(in);
        net.setLabels(label);
        net.computeGradientAndScore();

        INDArray grad = net.getFlattenedGradients();
        assertEquals(gradExp, grad);
    }


}
