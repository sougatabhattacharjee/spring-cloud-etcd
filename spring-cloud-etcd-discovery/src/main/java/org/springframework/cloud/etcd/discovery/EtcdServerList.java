/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.etcd.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.SneakyThrows;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;

/**
 * @author Spencer Gibb
 */
public class EtcdServerList extends AbstractServerList<EtcdServer> {

	private EtcdClient etcd;
	private EtcdDiscoveryProperties props;
	private String serviceId;

	public EtcdServerList() {
	}

	public EtcdServerList(EtcdClient etcd, EtcdDiscoveryProperties props, String serviceId) {
		this.etcd = etcd;
		this.props = props;
		this.serviceId = serviceId;
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		this.serviceId = clientConfig.getClientName();
	}

	@Override
	public List<EtcdServer> getInitialListOfServers() {
		return getServers();
	}

	@Override
	public List<EtcdServer> getUpdatedListOfServers() {
		return getServers();
	}

	@SneakyThrows
	private List<EtcdServer> getServers() {
		if (etcd == null) {
			return Collections.emptyList();
		}

		EtcdKeysResponse response = etcd
				.getDir(props.getDiscoveryPrefix() + "/" + serviceId).send().get();


		if (response.node.nodes == null || response.node.nodes.isEmpty()) {
			return Collections.emptyList();
		}

		List<EtcdServer> servers = new ArrayList<>();
		for (EtcdNode node : response.node.nodes) {
			String[] appInfo = getAppInfo(node.key);
			String[] strings = node.value.split(":");

			EtcdServer server = new EtcdServer(appInfo[0], appInfo[1], strings[0], strings[1]);
			servers.add(server);
		}

		return servers;
	}

	private String[] getAppInfo(String key) {
		String serviceNameId = key.replace(props.getDiscoveryPrefix(), "");
		return serviceNameId.substring(1).split("/");
	}
}
