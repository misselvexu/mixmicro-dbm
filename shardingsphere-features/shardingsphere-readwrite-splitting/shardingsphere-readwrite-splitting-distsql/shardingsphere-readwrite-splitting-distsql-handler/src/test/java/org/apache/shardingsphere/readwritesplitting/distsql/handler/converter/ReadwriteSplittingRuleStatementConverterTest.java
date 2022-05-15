/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.readwritesplitting.distsql.handler.converter;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.distsql.parser.segment.ReadwriteSplittingRuleSegment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class ReadwriteSplittingRuleStatementConverterTest {
    
    @Test
    public void assertEmptyRuleSegmentConvertResult() {
        ReadwriteSplittingRuleConfiguration actualEmptyRuleSegmentConvertResult = ReadwriteSplittingRuleStatementConverter
                .convert(Collections.emptyList());
        assertTrue(actualEmptyRuleSegmentConvertResult.getDataSources().isEmpty());
        assertTrue(actualEmptyRuleSegmentConvertResult.getLoadBalancers().isEmpty());
    }
    
    @Test
    public void assertSingleRuleSegmentConvertResult() {
        ReadwriteSplittingRuleSegment expectedSingleReadwriteSplittingRuleSegment = createReadwriteSplittingRuleSegment("static", "write_ds",
                Arrays.asList("read_ds_01", "read_ds_02"), "static_load_balancer_type", new Properties());
        ReadwriteSplittingRuleConfiguration actualSingleRuleSegmentConvertResult = ReadwriteSplittingRuleStatementConverter
                .convert(Collections.singleton(expectedSingleReadwriteSplittingRuleSegment));
        Collection<ReadwriteSplittingDataSourceRuleConfiguration> actualSingleRuleSegmentConvertResultDataSources = actualSingleRuleSegmentConvertResult.getDataSources();
        Map<String, ShardingSphereAlgorithmConfiguration> actualSingleRuleSegmentConvertResultLoadBalancers = actualSingleRuleSegmentConvertResult.getLoadBalancers();
        assertThat(actualSingleRuleSegmentConvertResultDataSources.size(), is(1));
        assertThat(actualSingleRuleSegmentConvertResultLoadBalancers.size(), is(1));
        ReadwriteSplittingDataSourceRuleConfiguration actualRuleConfig = actualSingleRuleSegmentConvertResultDataSources.iterator().next();
        assertThat(actualRuleConfig.getName(), is(expectedSingleReadwriteSplittingRuleSegment.getName()));
        String expectedLoadBalancerName = String.format("%s_%s", expectedSingleReadwriteSplittingRuleSegment.getName(), expectedSingleReadwriteSplittingRuleSegment.getLoadBalancer());
        assertThat(actualRuleConfig.getLoadBalancerName(), is(expectedLoadBalancerName));
        assertThat(actualRuleConfig.getWriteDataSourceName(), is(Optional.ofNullable(expectedSingleReadwriteSplittingRuleSegment.getWriteDataSource())));
        assertThat(convertToList(actualRuleConfig.getReadDataSourceNames().orElse(null)), is(expectedSingleReadwriteSplittingRuleSegment.getReadDataSources()));
        String actualLoadBalancerName = actualSingleRuleSegmentConvertResultLoadBalancers.keySet().iterator().next();
        assertThat(actualLoadBalancerName, is(expectedLoadBalancerName));
        ShardingSphereAlgorithmConfiguration actualSphereAlgorithmConfig = actualSingleRuleSegmentConvertResultLoadBalancers.get(actualLoadBalancerName);
        assertThat(actualSphereAlgorithmConfig.getType(), containsString(expectedSingleReadwriteSplittingRuleSegment.getLoadBalancer()));
        assertThat(actualSphereAlgorithmConfig.getProps(), is(expectedSingleReadwriteSplittingRuleSegment.getProps()));
    }
    
    @Test
    public void assertMultipleReadwriteSplittingRuleSegments() {
        List<ReadwriteSplittingRuleSegment> expectedMultipleReadwriteSplittingRuleSegments = Arrays.asList(
                createReadwriteSplittingRuleSegment("static", "write_ds", Arrays.asList("read_ds_01", "read_ds_02"), "static_load_balancer_type", new Properties()),
                createReadwriteSplittingRuleSegment("dynamic", "autoAwareResource", "dynamic_load_balancer", new Properties()));
        ReadwriteSplittingRuleConfiguration actualMultipleRuleSegmentConvertResult = ReadwriteSplittingRuleStatementConverter.convert(expectedMultipleReadwriteSplittingRuleSegments);
        Collection<ReadwriteSplittingDataSourceRuleConfiguration> actualMultipleRuleSegmentConvertResultDataSources = actualMultipleRuleSegmentConvertResult.getDataSources();
        Map<String, ShardingSphereAlgorithmConfiguration> actualMultipleRuleSegmentConvertResultLoadBalancers = actualMultipleRuleSegmentConvertResult.getLoadBalancers();
        assertThat(actualMultipleRuleSegmentConvertResultDataSources.size(), is(expectedMultipleReadwriteSplittingRuleSegments.size()));
        assertThat(actualMultipleRuleSegmentConvertResultLoadBalancers.size(), is(expectedMultipleReadwriteSplittingRuleSegments.size()));
        List<ReadwriteSplittingDataSourceRuleConfiguration> actualRuleConfigs = new ArrayList<>(actualMultipleRuleSegmentConvertResultDataSources);
        Stream.iterate(0, i -> i + 1)
                .limit(expectedMultipleReadwriteSplittingRuleSegments.size())
                .forEach(each -> {
                    ReadwriteSplittingRuleSegment expectedReadwriteSplittingRuleSegment = expectedMultipleReadwriteSplittingRuleSegments.get(each);
                    ReadwriteSplittingDataSourceRuleConfiguration actualRuleConfig = actualRuleConfigs.get(each);
                    assertThat(actualRuleConfig.getName(), is(expectedReadwriteSplittingRuleSegment.getName()));
                    String expectedLoadBalancerName = String.format("%s_%s", expectedReadwriteSplittingRuleSegment.getName(), expectedReadwriteSplittingRuleSegment.getLoadBalancer());
                    assertThat(actualRuleConfig.getLoadBalancerName(), is(expectedLoadBalancerName));
                    assertThat(actualRuleConfig.getWriteDataSourceName().orElse(null), is(expectedReadwriteSplittingRuleSegment.getWriteDataSource()));
                    assertThat(convertToList(actualRuleConfig.getReadDataSourceNames().orElse(null)),
                            equalTo(expectedReadwriteSplittingRuleSegment.getReadDataSources() == null
                                    ? Collections.emptyList()
                                    : expectedReadwriteSplittingRuleSegment.getReadDataSources()));
                    assertTrue(actualMultipleRuleSegmentConvertResultLoadBalancers.containsKey(expectedLoadBalancerName));
                    ShardingSphereAlgorithmConfiguration actualSphereAlgorithmConfig = actualMultipleRuleSegmentConvertResultLoadBalancers.get(actualRuleConfig.getLoadBalancerName());
                    assertThat(actualSphereAlgorithmConfig.getType(), containsString(expectedReadwriteSplittingRuleSegment.getLoadBalancer()));
                    assertThat(actualSphereAlgorithmConfig.getProps(), is(expectedReadwriteSplittingRuleSegment.getProps()));
                });
    }
    
    private ReadwriteSplittingRuleSegment createReadwriteSplittingRuleSegment(final String name, final String writeDataSource, final List<String> readDataSources, final String loadBalancerTypeName,
                                                                              final Properties props) {
        return new ReadwriteSplittingRuleSegment(name, writeDataSource, readDataSources, loadBalancerTypeName, props);
    }
    
    private ReadwriteSplittingRuleSegment createReadwriteSplittingRuleSegment(final String name, final String autoAwareResource, final String loadBalancer, final Properties props) {
        return new ReadwriteSplittingRuleSegment(name, autoAwareResource, loadBalancer, props);
    }
    
    private Collection<String> convertToList(final String readDataSourceNames) {
        return Strings.isNullOrEmpty(readDataSourceNames) ? Collections.emptyList() : Splitter.on(",").trimResults().splitToList(readDataSourceNames);
    }
}
