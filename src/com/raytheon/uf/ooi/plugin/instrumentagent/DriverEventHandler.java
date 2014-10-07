package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.camel.ProducerTemplate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class DriverEventHandler implements Observer {
	
	protected ProducerTemplate producer;
	protected InstrumentAgent agent;
	protected String sensor;

	protected IUFStatusHandler status = UFStatus.getHandler(Ingest.class);

    public DriverEventHandler(InstrumentAgent agent, ProducerTemplate producer, String sensor) {
    	this.agent = agent;
    	this.producer = producer;
    	this.sensor = sensor;
    }
    
    @Override
    public void update(Observable o, Object arg) {
        status.handle(Priority.DEBUG, "EVENTOBSERVER GOT: " + arg);
        try {
			Map<String, Object> event = JsonHelper.toMap((String) arg);
            switch ((String)event.get("type")) {
            	case Constants.CONFIG_CHANGE_EVENT:
                case Constants.STATE_CHANGE_EVENT:
                    agent.getOverallState();
                    break;
                case Constants.SAMPLE_EVENT:
                	Map<String, Object> particle =
                		JsonHelper.toMap((String) event.get("value"));
                	if (particle.get("stream_name").equals("raw")) {
                		// TODO handle raw
                	} else {
                		// inject event time into particle
                		producer.sendBodyAndHeader(particle, "sensor", sensor);
                	}
                    break;
                case Constants.DRIVER_ASYNC_EVENT:
                case Constants.DRIVER_EXCEPTION:
                	int transactionId = (int) event.get("transaction_id");
                	this.agent.transactionMap.put(transactionId, (String) arg);
                	break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
