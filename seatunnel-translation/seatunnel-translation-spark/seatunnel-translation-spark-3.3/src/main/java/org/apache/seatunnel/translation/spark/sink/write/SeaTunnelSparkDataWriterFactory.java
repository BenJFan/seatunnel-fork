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

package org.apache.seatunnel.translation.spark.sink.write;

import org.apache.seatunnel.api.sink.DefaultSinkWriterContext;
import org.apache.seatunnel.api.sink.SeaTunnelSink;
import org.apache.seatunnel.api.sink.SinkCommitter;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;

import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.write.DataWriter;
import org.apache.spark.sql.connector.write.DataWriterFactory;
import org.apache.spark.sql.connector.write.streaming.StreamingDataWriterFactory;

import java.io.IOException;
import java.sql.DriverManager;

public class SeaTunnelSparkDataWriterFactory<CommitInfoT, StateT>
        implements DataWriterFactory, StreamingDataWriterFactory {

    static {
        // Load DriverManager first to avoid deadlock between DriverManager's
        // static initialization block and specific driver class's static
        // initialization block when two different driver classes are loading
        // concurrently using Class.forName while DriverManager is uninitialized
        // before.
        //
        // This could happen in JDK 8 but not above as driver loading has been
        // moved out of DriverManager's static initialization block since JDK 9.
        DriverManager.getDrivers();
    }

    private final SeaTunnelSink<SeaTunnelRow, StateT, CommitInfoT, ?> sink;
    private final CatalogTable catalogTable;
    private final String jobId;

    public SeaTunnelSparkDataWriterFactory(
            SeaTunnelSink<SeaTunnelRow, StateT, CommitInfoT, ?> sink,
            CatalogTable catalogTable,
            String jobId) {
        this.sink = sink;
        this.catalogTable = catalogTable;
        this.jobId = jobId;
    }

    @Override
    public DataWriter<InternalRow> createWriter(int partitionId, long taskId) {
        SinkWriter.Context context = new DefaultSinkWriterContext(jobId, (int) taskId);
        SinkWriter<SeaTunnelRow, CommitInfoT, StateT> writer;
        SinkCommitter<CommitInfoT> committer;
        try {
            writer = sink.createWriter(context);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SinkWriter.", e);
        }
        try {
            committer = sink.createCommitter().orElse(null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SinkCommitter.", e);
        }
        return new SeaTunnelSparkDataWriter<>(
                writer, committer, catalogTable.getSeaTunnelRowType(), 0);
    }

    @Override
    public DataWriter<InternalRow> createWriter(int partitionId, long taskId, long epochId) {
        return createWriter(partitionId, taskId);
    }
}
