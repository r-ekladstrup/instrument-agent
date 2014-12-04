package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.raytheon.uf.common.dataplugin.sensorreading.SensorReadingRecord;
import com.raytheon.uf.edex.ooi.decoder.dataset.AbstractParticleDecoder;
import com.raytheon.uf.edex.ooi.decoder.dataset.HashingException;

/**
 * @author pcable
 * 
 */
public class Ingest extends AbstractParticleDecoder implements Processor {

    public void process(Exchange exchange) throws Exception {
        String sensor = (String) exchange.getIn().getHeader(AbstractParticleDecoder.EXCHANGE_SENSOR_HEADER);

        @SuppressWarnings("unchecked")
        Map<String, Object> particle = exchange.getIn().getBody(Map.class);

        SensorReadingRecord readings[] = { parseMap("streaming", sensor, particle) };

        exchange.getOut().setBody(readings);
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