/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.emqx.source;

import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.connectors.seatunnel.emqx.state.EmqxSourceState;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EmqxSourceSplitEnumerator
        implements SourceSplitEnumerator<EmqxSourceSplit, EmqxSourceState> {

    private final SourceSplitEnumerator.Context<EmqxSourceSplit> context;

    private final List<EmqxSourceSplit> splits;

    EmqxSourceSplitEnumerator(SourceSplitEnumerator.Context<EmqxSourceSplit> enumeratorContext) {
        this.context = enumeratorContext;
        this.splits = new ArrayList<>(context.currentParallelism());
        for (int i = 0; i < context.currentParallelism(); i++) {
            splits.add(new EmqxSourceSplit(String.valueOf(i)));
        }
    }

    @Override
    public void open() {}

    @Override
    public void run() {}

    @Override
    public void close() {}

    @Override
    public void addSplitsBack(List<EmqxSourceSplit> splits, int subtaskId) {
        if (!splits.isEmpty()) {
            splits.add(subtaskId, splits.get(0));
        }
    }

    @Override
    public int currentUnassignedSplitSize() {
        return 0;
    }

    @Override
    public void handleSplitRequest(int subtaskId) {
        assignSplit(subtaskId);
    }

    @Override
    public void registerReader(int subtaskId) {
        assignSplit(subtaskId);
    }

    private void assignSplit(int subtaskId) {
        if (splits.get(subtaskId) != null) {
            context.assignSplit(subtaskId, splits.get(subtaskId));
        }
    }

    @Override
    public EmqxSourceState snapshotState(long checkpointId) throws Exception {
        return new EmqxSourceState();
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
        // Do nothing
    }
}
