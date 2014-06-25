package com.dianping.cat.agent.monitor.paas;

import java.util.List;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.helper.Threads;
import org.unidal.helper.Threads.Task;
import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Cat;
import com.dianping.cat.agent.monitor.DataEntity;
import com.dianping.cat.agent.monitor.DataSender;
import com.dianping.cat.message.Transaction;

public class PaasTask implements Task, Initializable {

	@Inject
	private DataSender m_dataSender;

	@Inject
	private DataFetcher m_dataFetcher;

	private static final int DURATION = 5 * 1000;

	@Override
	public void initialize() throws InitializationException {
		Threads.forGroup("Cat").start(this);
	}

	@Override
	public void run() {
		boolean active = true;

		while (active) {
			long current = System.currentTimeMillis();
			Transaction t = Cat.newTransaction("Paas", "Agent");

			try {
				List<String> instances = m_dataFetcher.buildInstances();
				for (String instance : instances) {
					List<DataEntity> entities = m_dataFetcher.buildData(instance);

					m_dataSender.put(entities);
				}
				t.setStatus(Transaction.SUCCESS);
			} catch (Exception e) {
				t.setStatus(e);
				Cat.logError(e);
			} finally {
				t.complete();
			}

			long duration = System.currentTimeMillis() - current;

			try {
				if (duration < DURATION) {
					Thread.sleep(DURATION - duration);
				}
			} catch (InterruptedException e) {
				active = false;
			}
		}
	}

	@Override
	public String getName() {
		return "phoenix-task";
	}

	@Override
	public void shutdown() {

	}

}
