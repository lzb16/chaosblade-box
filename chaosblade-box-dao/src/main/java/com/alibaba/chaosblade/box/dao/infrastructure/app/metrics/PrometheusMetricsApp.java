/*
 * Copyright 2025 The ChaosBlade Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.chaosblade.box.dao.infrastructure.app.metrics;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.chaosblade.box.common.app.sdk.*;
import com.alibaba.chaosblade.box.common.app.sdk.annotations.Author;
import com.alibaba.chaosblade.box.common.app.sdk.annotations.ChaosApplication;
import com.alibaba.chaosblade.box.common.app.sdk.annotations.ChaosArgs;
import com.alibaba.chaosblade.box.common.app.sdk.annotations.ChaosFunction;
import com.alibaba.chaosblade.box.common.app.sdk.constants.ChaosAppType;
import com.alibaba.chaosblade.box.common.app.sdk.scope.Host;
import com.alibaba.chaosblade.box.common.app.sdk.scope.Scope;
import com.alibaba.chaosblade.box.common.infrastructure.metric.ChaosMetricEntity;
import com.alibaba.chaosblade.box.common.infrastructure.util.HttpClient;
import com.alibaba.chaosblade.box.common.infrastructure.util.JacksonUtils;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.math.BigDecimal;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/** @author xf.yefei */
@ChaosApplication(name = "普罗米修斯监控", code = "metrics-prometheus", type = ChaosAppType.COMPONENT)
@Author(forPublic = true, userId = "-1")
public class PrometheusMetricsApp extends BaseChaosApp {

  @Override
  public void setContext(ChaosAppContext context) {
    super.setContext(context);
  }

  @Override
  public ChaosAppContext getContext() {
    return super.getContext();
  }

  @Override
  public void setChaosToolkit(ChaosToolkit toolkit) {
    super.setChaosToolkit(toolkit);
  }

  @Override
  public ChaosToolkit toolkit() {
    return super.toolkit();
  }

  @ChaosFunction(
      name = "HOST CPU USAGERATE",
      code = "cpu-usage",
      description = "",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse cpuUsage(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "100 - (avg(irate(node_cpu_seconds_total{mode=~'idle', job=~'%s', instance=~'%s.*'}[5m])) by (instance) * 100)",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "HOST MEM USAGERATE",
      code = "memory-usage",
      description = "",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse memoryUsage(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {

    String query =
        String.format(
            "(node_memory_MemTotal_bytes{instance=~'%s.*', job=~'%s'} - node_memory_MemFree_bytes{instance=~'%s.*', job=~'%s'} - node_memory_Buffers_bytes{instance=~'%s.*', job=~'%s'} - node_memory_Cached_bytes{instance=~'%s.*', job=~'%s'}) / node_memory_MemTotal_bytes{instance=~'%s.*', job=~'%s'} * 100",
            scope.getIp(),
            job,
            scope.getIp(),
            job,
            scope.getIp(),
            job,
            scope.getIp(),
            job,
            scope.getIp(),
            job);
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "HOST DISK LOAD",
      code = "io-load",
      description = "",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse ioLoad(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {

    String query =
        String.format(
            "rate(node_disk_io_time_seconds_total{instance=~'%s.*', job=~'%s'}[5m])",
            scope.getIp(), job);
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "HOST DISK USAGERATE",
      code = "disk-usage",
      description = "主机磁盘使用率",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse diskUsage(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {

    String query =
        String.format(
            "100 - (sum(node_filesystem_free_bytes{fstype=~'ext.?|xfs', instance=~'%s.*', job=~'%s'}) by (instance, job) / sum(node_filesystem_size_bytes{fstype=~'ext.?|xfs', instance=~'%s.*', job=~'%s'}) by (instance, job) * 100)",
            scope.getIp(), job, scope.getIp(), job);
    return invoke0(scope, from, to, api, query);
  }

  // ==================== ZooKeeper Metrics ====================

  @ChaosFunction(
      name = "ZOOKEEPER SERVER STATE",
      code = "zk-server-state",
      description = "ZooKeeper服务器状态，1表示Leader",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkServerState(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "max by(instance) (zookeeper_server_state{state='leader', job=~'%s', instance=~'%s.*'})",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER AVG LATENCY",
      code = "zk-avg-latency",
      description = "ZooKeeper平均延迟(ms)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkAvgLatency(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_avg_latency{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER MAX LATENCY",
      code = "zk-max-latency",
      description = "ZooKeeper最大延迟(ms)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkMaxLatency(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_max_latency{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER OUTSTANDING REQUESTS",
      code = "zk-outstanding-requests",
      description = "ZooKeeper待处理请求数",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkOutstandingRequests(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_outstanding_requests{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER CONNECTIONS",
      code = "zk-connections",
      description = "ZooKeeper当前连接数",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkConnections(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_num_alive_connections{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER CONNECTION DROP RATE",
      code = "zk-connection-drop-rate",
      description = "ZooKeeper连接断开速率(次/秒)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkConnectionDropRate(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "rate(zookeeper_connection_drop_count{job=~'%s', instance=~'%s.*'}[5m])",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER SYNCED FOLLOWERS",
      code = "zk-synced-followers",
      description = "ZooKeeper已同步Follower数量(仅Leader有值)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkSyncedFollowers(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_synced_followers{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER PENDING SYNCS",
      code = "zk-pending-syncs",
      description = "ZooKeeper待同步数量(仅Leader有值)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkPendingSyncs(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_pending_syncs{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER PACKETS RECEIVED RATE",
      code = "zk-packets-received-rate",
      description = "ZooKeeper接收包速率(个/秒)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkPacketsReceivedRate(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "rate(zookeeper_packets_received{job=~'%s', instance=~'%s.*'}[5m])",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER PACKETS SENT RATE",
      code = "zk-packets-sent-rate",
      description = "ZooKeeper发送包速率(个/秒)",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkPacketsSentRate(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "rate(zookeeper_packets_sent{job=~'%s', instance=~'%s.*'}[5m])",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER WATCH COUNT",
      code = "zk-watch-count",
      description = "ZooKeeper Watch数量",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkWatchCount(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_watch_count{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @ChaosFunction(
      name = "ZOOKEEPER ZNODE COUNT",
      code = "zk-znode-count",
      description = "ZooKeeper ZNode数量",
      categories = "1216627609183686658",
      mode = InvokeMode.ONCE)
  public ChaosAppResponse zkZnodeCount(
      Scope scope,
      @ChaosArgs(name = "开始时间", alias = "from") String from,
      @ChaosArgs(name = "结束时间", alias = "to") String to,
      @ChaosArgs(name = "job", alias = "job") String job,
      @ChaosArgs(name = "api", alias = "api") String api) {
    String query =
        String.format(
            "zookeeper_znode_count{job=~'%s', instance=~'%s.*'}",
            job, scope.getIp());
    return invoke0(scope, from, to, api, query);
  }

  @NotNull private ChaosAppResponse invoke0(Scope scope, String from, String to, String api, String query) {

    List<ChaosMetricEntity> chaosMetricEntities = new ArrayList<>();

    String step = "14";

    Map<String, String> params = new HashMap(8);
    params.put("query", query);
    params.put("start", String.valueOf(Long.parseLong(from) / 1000));
    params.put("end", String.valueOf(Long.parseLong(to) / 1000));
    params.put("step", step);

    Optional<String> optional = HttpClient.postForm(api, params, new TypeReference<String>() {});

    optional.ifPresent(
        s -> {
          JsonNode jsonNode = JacksonUtils.readTree(s);
          ArrayNode result = (ArrayNode) jsonNode.get("data").get("result");
          if (CollUtil.isNotEmpty(result)) {
            for (JsonNode node : result) {
              String metric = node.get("metric").toString();
              // values
              ArrayNode values = (ArrayNode) node.get("values");
              for (JsonNode dot : values) {
                ChaosMetricEntity chaosMetricEntity = new ChaosMetricEntity();
                String date = dot.get(0).asText();
                String value = dot.get(1).asText();

                chaosMetricEntity.setMetric(metric);
                chaosMetricEntity.setTimestamp(
                    DateUtil.date(new BigDecimal(date).longValue() * 1000).getTime());
                chaosMetricEntity.setValue(NumberUtil.parseNumber(value));
                Host host = new Host();
                host.setIp(scope.getIp());
                chaosMetricEntity.setHost(host);
                chaosMetricEntities.add(chaosMetricEntity);
              }
            }
          }
        });

    ChaosAppResponse chaosAppResponse = new ChaosAppResponse();
    Map<String, Object> data = new HashMap<>();
    data.put("response", chaosMetricEntities);
    chaosAppResponse.setData(data);
    chaosAppResponse.setSuccess(true);
    return chaosAppResponse;
  }
}
