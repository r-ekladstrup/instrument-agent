package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class DriverEventHandler implements Observer {
	
	@EndpointInject(uri="jms-durable:queue:particle_data")
	protected ProducerTemplate producer;
	
	protected String sensor;

	protected IUFStatusHandler status = UFStatus.getHandler(Ingest.class);

    public DriverEventHandler() {}
    
    @Override
    public void update(Observable o, Object arg) {
        status.handle(Priority.DEBUG, "EVENTOBSERVER GOT: " + arg);
        try {
			Map<String, Object> event = JsonHelper.toMap((String) arg);
            switch ((String)event.get("type")) {
                case Constants.STATE_CHANGE_EVENT:
                    // TODO update state in proxy
                    break;
                case Constants.SAMPLE_EVENT:
                	Map<String, Object> particle =
                		JsonHelper.toMap((String) event.get("value"));
                	if (particle.get("stream_name").equals("raw")) {
                		// TODO handle raw
                	} else {
                		// inject event time into particle
                		particle.put("time", event.get("time"));
                		producer.sendBodyAndHeader(particle, "sensor", sensor);
                	}
                    break;
                case Constants.CONFIG_CHANGE_EVENT:
                	// TODO update state in proxy
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void setSensor(String sensor) {
		this.sensor = sensor;
	}
	
	public String getSensor() {
		return sensor;
	}
}
