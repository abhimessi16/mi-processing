/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.misinformationinvestigate.processing;

import com.google.gson.Gson;
import com.misinformationinvestigate.processing.helper.RandomLiveStreamGenerator;
import com.misinformationinvestigate.processing.models.*;
import com.misinformationinvestigate.processing.utils.AsyncFactCheckRequest;
import com.misinformationinvestigate.processing.utils.AsyncPredictionRequest;
import com.misinformationinvestigate.processing.utils.AsyncSSEFactCheckRequest;
import com.misinformationinvestigate.processing.utils.AsyncSTTRequest;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.misinformationinvestigate.processing.utils.CommonUtils.*;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class DataStreamJob {

	final static Gson gson = new Gson();
	final static Properties properties = new Properties();

	public static void main(String[] args) throws Exception {

		try(InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("application.properties")){
			properties.load(is);
		}catch (IOException io){
			System.out.println(io.getMessage());
		}

		// Sets up the execution environment, which is the main entry point
		// to building Flink applications.
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
		env.setParallelism(1);

		KafkaSource<String> liveFeedSource = KafkaSource.<String>builder()
				.setBootstrapServers(properties.getProperty("KAFKA_BOOTSTRAP_SERVERS"))
				.setTopics(properties.getProperty("KAFKA_TOPICS"))
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		DataStream<LiveStream> liveFeedStream = env.fromSource(
				liveFeedSource,
				WatermarkStrategy.noWatermarks(),
				"Live Feed Source")
				.map(
						liveStream -> gson.fromJson(liveStream, LiveStream.class))
				.name("Convert to Live Stream object");

        DataStream<LiveStream> reducedLiveStream = liveFeedStream.keyBy(LiveStream::getSessionId)
				.window(TumblingProcessingTimeWindows.of(Duration.ofSeconds(4)))
				.reduce((ReduceFunction<LiveStream>) (liveStream, t1) -> {
                                        liveStream.setAudioData(mergeTwoShortArrays(
                                                liveStream.getAudioData(), t1.getAudioData()
                                        ));
                                        return LiveStream.builder()
												.audioData(mergeTwoShortArrays(liveStream.getAudioData(), t1.getAudioData()))
												.sessionId(liveStream.getSessionId())
												.source(liveStream.getSource())
												.build();
                                    })
				.name("Reduce multiple PCM byte arrays to single array.");

		DataStream<LiveStreamFloat> liveStreamFloatDataStream = reducedLiveStream
				.map(liveStream -> LiveStreamFloat.builder()
						.source(liveStream.getSource())
						.sessionId(liveStream.getSessionId())
						.audioData(convertShortToFloatArray(liveStream.getAudioData()))
						.build())
				.name("Convert PCM short to PCM float");

		DataStream<LiveStreamText> liveStreamTextDataStream = AsyncDataStream.orderedWait(
				liveStreamFloatDataStream, new AsyncSTTRequest(properties), 600000, TimeUnit.MILLISECONDS, 10)
				.keyBy(LiveStreamText::getSessionId)
				.window(TumblingProcessingTimeWindows.of(Duration.ofSeconds(20)))
				.reduce(new ReduceFunction<LiveStreamText>() {
					@Override
					public LiveStreamText reduce(LiveStreamText liveStreamText, LiveStreamText t1) throws Exception {
						char c1 = liveStreamText.getAudioData()
								.charAt(liveStreamText.getAudioData().length() - 1);
						char c2 = t1.getAudioData()
								.charAt(0);
						String audioData;
						if(c1 == ' ' || c2 == ' ')
							audioData = liveStreamText.getAudioData().concat(t1.getAudioData());
						else{
							audioData = liveStreamText.getAudioData() + " " + t1.getAudioData();
						}
						return LiveStreamText.builder()
								.audioData(audioData)
								.source(liveStreamText.getSource())
								.sessionId(liveStreamText.getSessionId())
								.build();
					}
				})
				.name("STT Combined results for predictions");

//		liveStreamTextDataStream.print();

		DataStream<LiveStreamPrediction>  liveStreamPredictionDataStream = AsyncDataStream.unorderedWait(
				liveStreamTextDataStream, new AsyncPredictionRequest(properties), 600000, TimeUnit.MILLISECONDS, 100)
						.name("Prediction results");

		liveStreamPredictionDataStream.print();

		DataStream<LiveStreamFactChecked> factCheckedFakeNews = AsyncDataStream.unorderedWait(
				liveStreamPredictionDataStream
						.filter(LiveStreamPrediction::isFakeNews), new AsyncFactCheckRequest(properties), 600000, TimeUnit.MILLISECONDS, 100)
				.filter(LiveStreamFactChecked::isFakeNews)
				.name("Fact checked news articles which are fake");

		factCheckedFakeNews.print();

		// store the fact checked fake news into databases
		// for now only relational, let's do graph db later
		// sink to db

		factCheckedFakeNews.addSink(JdbcSink.sink(
				"INSERT INTO FactCheckedNews (source, sessionId, audioData, isFakeNews) " +
						"values (?, ?, ?, ?)",
				(preparedStatement, liveStreamPrediction) -> {
					preparedStatement.setString(1, liveStreamPrediction.getSource());
					preparedStatement.setString(2, liveStreamPrediction.getSessionId());
					preparedStatement.setString(3, liveStreamPrediction.getAudioData());
					preparedStatement.setBoolean(4, liveStreamPrediction.isFakeNews());
				},
				JdbcExecutionOptions.builder()
						.withBatchSize(100)
						.withBatchIntervalMs(100)
						.withMaxRetries(5)
						.build(),
				new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
						.withUrl(properties.getProperty("DB_URL"))
						.withDriverName("org.postgresql.Driver")
						.withUsername(properties.getProperty("DB_USERNAME"))
						.withPassword(properties.getProperty("DB_PASSWORD"))
						.build()
		));

		// send data to sse endpoint for streaming
		AsyncDataStream.unorderedWait(
				factCheckedFakeNews,
				new AsyncSSEFactCheckRequest(properties),
				600000,
				TimeUnit.SECONDS
		);

		env.execute("Misinformation Investigate - Processing");
	}
}
