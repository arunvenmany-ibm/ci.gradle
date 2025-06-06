/**
 * (C) Copyright IBM Corporation 2019, 2025.
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
package io.openliberty.tools.gradle.extensions

class CompileJSPExtension {
 
    // Sets the JSP version to use. Valid values are `2.2` or `2.3`. The default value is `2.3`.
    String jspVersion

    // Maximum time to wait (in seconds) for all the JSP files to compile. 
    // The server is stopped and the goal ends after this specified time. The default value is 30 seconds.
    int jspCompileTimeout = 40
}