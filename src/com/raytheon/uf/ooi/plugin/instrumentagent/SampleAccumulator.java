package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;

import com.raytheon.uf.common.dataplugin.sensorreading.SensorReadingRecord;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.ooi.decoder.dataset.AbstractParticleDecoder;
import com.raytheon.uf.edex.ooi.decoder.dataset.HashingException;

public class SampleAccumulator extends AbstractParticleDecoder implements Runnable {
    private IUFStatusHandler statusHandler = UFStatus.getHandler(this.getClass());
    private List<SensorReadingRecord> sampleList = new LinkedList<SensorReadingRecord>();
    private long PUBLISH_INTERVAL = 5000;

    @EndpointInject(uri = "direct-vm:persistIndexAlert")
    protected ProducerTemplate producer;

    public synchronized void process(Map<String, Object> particle, String sensor) throws Exception {
        SensorReadingRecord record = parseMap("streaming", sensor, particle);
        sampleList.add(record);
    }

    public void publish() {
        Object records[];
        synchronized (sampleList) {
            records = sampleList.toArray();
            sampleList.clear();
        }

        if (records.length > 0) {
            statusHandler.handle(Priority.INFO, "Going to publish " + records.length + " particles");
            producer.sendBody(records);
        }
    }

    public void run() {
        while (true) {
            try {
                publish();
                Thread.sleep(PUBLISH_INTERVAL);
            } catch (Exception e) {
                statusHandler.handle(Priority.CRITICAL, "Ignoring exception in InstrumentAgent publish loop: " + e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.edex.ooi.decoder.dataset.AbstractParticleDecoder#
     * getHashColumn(java.util.Map)
     */
    @Override
    protected String getHashColumn(Map<String, Object> flattenedValues)
            throws HashingException {
        return null;
    }
}
