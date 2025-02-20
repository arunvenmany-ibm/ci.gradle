/**
 * (C) Copyright IBM Corporation 2015, 2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel

class DumpTask extends AbstractServerTask {

    DumpTask() {
        configure({
            description = 'Dumps diagnostic information from the Liberty server into an archive.'
            group = 'Liberty'
        })
    }

    @TaskAction
    void dump() {
        def params = buildLibertyMap(project);
        if (server.dumpLiberty.archive != null && server.dumpLiberty.archive.length() != 0) {
            params.put('archive', new File(server.dumpLiberty.archive))
        }
        if (server.dumpLiberty.include != null && server.dumpLiberty.include.length() != 0) {
            params.put('include',server.dumpLiberty.include)
        }
        executeServerCommand(project, 'dump', params)
    }
}
