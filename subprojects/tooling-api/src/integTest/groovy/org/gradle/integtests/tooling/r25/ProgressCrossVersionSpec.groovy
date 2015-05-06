/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.integtests.tooling.r25

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressEventType
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.build.BuildProgressEvent
import org.gradle.tooling.events.task.TaskProgressEvent
import org.gradle.tooling.events.test.TestProgressEvent

@ToolingApiVersion(">=2.5")
@TargetGradleVersion(">=2.5")
class ProgressCrossVersionSpec extends ToolingApiSpecification {

    def "register for all progress events at once"() {
        given:
        goodCode()

        when: "registering for all progress event types"
        List<ProgressEvent> result = new ArrayList<ProgressEvent>()
        withConnection {
            ProjectConnection connection ->
                connection.newBuild().forTasks('test').addProgressListener(new ProgressListener() {
                    @Override
                    void statusChanged(ProgressEvent event) {
                        result << event
                    }
                }, EnumSet.allOf(ProgressEventType)).run()
        }

        then: "all progress events must be forwarded to the attached listener"
        result.size() > 0
        result.findAll { it instanceof TestProgressEvent }.size() > 0
        result.findAll { it instanceof TaskProgressEvent }.size() > 0
        result.findAll { it instanceof BuildProgressEvent }.size() > 0
        result.findIndexOf { it instanceof BuildProgressEvent } < result.findIndexOf { it instanceof TaskProgressEvent }
        result.findIndexOf { it instanceof TaskProgressEvent } < result.findIndexOf { it instanceof TestProgressEvent }
        result.findLastIndexOf { it instanceof TaskProgressEvent } > result.findLastIndexOf { it instanceof TestProgressEvent }
        result.findLastIndexOf { it instanceof BuildProgressEvent } > result.findLastIndexOf { it instanceof TaskProgressEvent }
    }

    def "register for subset of progress events at once"() {
        given:
        goodCode()

        when: "registering for subset of progress event types"
        List<ProgressEvent> result = new ArrayList<ProgressEvent>()
        withConnection {
            ProjectConnection connection ->
                connection.newBuild().forTasks('test').addProgressListener(new ProgressListener() {
                    @Override
                    void statusChanged(ProgressEvent event) {
                        result << event
                    }
                }, EnumSet.of(ProgressEventType.TEST)).run()
        }

        then: "only the matching progress events must be forwarded to the attached listener"
        result.size() > 0
        result.findAll { it instanceof TestProgressEvent }.size() > 0
        result.findAll { it instanceof TaskProgressEvent }.isEmpty()
        result.findAll { it instanceof BuildProgressEvent }.isEmpty()
    }

    def goodCode() {
        buildFile << """
            apply plugin: 'java'
            repositories { mavenCentral() }
            dependencies { testCompile 'junit:junit:4.12' }
            compileTestJava.options.fork = true
        """

        file("src/test/java/example/MyTest.java") << """
            package example;
            public class MyTest {
                @org.junit.Test public void foo() throws Exception {
                     org.junit.Assert.assertEquals(1, 1);
                }
            }
        """
    }

}