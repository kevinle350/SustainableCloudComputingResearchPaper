package org.cloudbus.cloudsim.tieredconfigurations;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.visualdata.DatacenterSelectionData;
import org.cloudbus.cloudsim.visualdata.CloudletExecutionData;

import org.cloudbus.cloudsim.workload.CloudletDataParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PowerMain {
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter highResDatacenter;
    private static Datacenter mediumResDatacenter;
    private static Datacenter lowResDatacenter;

    public static void main(String[] args) {
        try {
            // Step 1: Initialize CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUsers, calendar, traceFlag);

            // Step 2: Create fixed datacenters
            DatacenterFactory datacenterFactory = new DatacenterFactory();

            // Step 2: Initialize Power Data and Simulation Manager
            PowerData initialPowerData = new PowerData(100, 100);
            RealTimeSimulationManager simulationManager = new RealTimeSimulationManager("SimulationManager", initialPowerData, datacenterFactory);
            CloudSim.addEntity(simulationManager);

            // Step 4: Create broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Step 5: Create VMs and Cloudlets
            vmList = new ArrayList<>();
            vmList.add(createVM(brokerId, 0));
            vmList.add(createVM(brokerId, 1));
            broker.submitGuestList(vmList);

            cloudletList = new ArrayList<>();
            int id = 0;
            cloudletList.add(createCloudlet(id++, brokerId, 0, 1000000));
            cloudletList.add(createCloudlet(id++, brokerId, 0, 10000000));
            cloudletList.add(createCloudlet(id++, brokerId, 0, 100000000));
            cloudletList.add(createCloudlet(id++, brokerId, 1, 1000000));
            cloudletList.add(createCloudlet(id++, brokerId, 1, 10000000));
            cloudletList.add(createCloudlet(id++, brokerId, 1, 100000000));
            broker.submitCloudletList(cloudletList);

            // Step 6: Start Simulation
            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            // Step 7: Print Results
            printCloudletResults(broker.getCloudletReceivedList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Datacenter selectDatacenterBasedOnPowerData(PowerData powerData) {
        double fossilFreePercentage = powerData.getFossilFreePercentage();
        if (fossilFreePercentage > 70) {
            return highResDatacenter;
        } else if (fossilFreePercentage > 35) {
            return mediumResDatacenter;
        } else {
            return lowResDatacenter;
        }
    }

    public static Vm createVM(int brokerId, int vmid) {
        int mips = 1000;
        long size = 10000; // Image size (MB)
        int ram = 512; // RAM (MB)
        long bw = 1000; // Bandwidth
        int pesNumber = 1; // Number of CPUs
        String vmm = "Xen"; // VMM name

        // Create the VM
        return new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
    }

    public static Cloudlet createCloudlet(int id, int brokerId, int vmid, long length) {
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet cloudlet = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
        cloudlet.setUserId(brokerId);
        cloudlet.setGuestId(vmid);
        return cloudlet;
    }

    private static void printCloudletResults(List<Cloudlet> cloudlets) {
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID    STATUS    Datacenter ID    VM ID    Time    Start Time    Finish Time");
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                System.out.printf("%10d    SUCCESS    %12d    %5d    %4.2f    %9.2f    %10.2f%n",
                        cloudlet.getCloudletId(), cloudlet.getResourceId(), cloudlet.getVmId(),
                        cloudlet.getActualCPUTime(), cloudlet.getExecStartTime(), cloudlet.getFinishTime());
            }
        }

        // Bar/Scatter Plot for Cloudlet Execution Data over Time
        String filePath = "./cloudletExecutionTime.csv";
        CloudletExecutionData.exportCloudletExecutionTime(cloudlets, filePath);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker");
    }
}
