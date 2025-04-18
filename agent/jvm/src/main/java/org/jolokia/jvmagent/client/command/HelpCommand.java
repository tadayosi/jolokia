package org.jolokia.jvmagent.client.command;

/*
 * Copyright 2009-2018 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.InvocationTargetException;

import org.jolokia.server.core.Version;
import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.jvmagent.client.util.OptionsAndArgs;
import org.jolokia.jvmagent.client.util.VirtualMachineHandlerOperations;

/**
 * Print out usage information
 *
 * @author roland
 * @author nevenr
 * @since 06.10.11
 */
public class HelpCommand extends AbstractBaseCommand {

    /** {@inheritDoc} */
    @Override
    String getName() {
        return "help";
    }

    /** {@inheritDoc} */
    @Override
    int execute(OptionsAndArgs pOpts, Object pVm, VirtualMachineHandlerOperations pHandler) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        printUsage();
        return 0;
    }

    /**
     * Print out usage
     */
    @SuppressWarnings({"PMD.SystemPrintln","PMD.AvoidDuplicateLiterals"})
    static void printUsage() {
        String jar = OptionsAndArgs.lookupJarFile().getName();
        System.out.println(
"Jolokia Agent Launcher\n" +
"======================\n" +
"\n" +
"Usage: java -jar " + jar + " [options] <command> <pid/regexp>\n" +
"\n" +
"where <command> is one of\n" +
"    start     -- Start a Jolokia agent for the process specified\n" +
"    stop      -- Stop a Jolokia agent for the process specified\n" +
"    status    -- Show status of an (potentially) attached agent\n" +
"    toggle    -- Toggle between start/stop (default when no command is given)\n" +
"    list      -- List all attachable Java processes (default when no argument is given at all)\n" +
"    encrypt   -- Encrypt a password which is given as argument or read from standard input\n" +
"\n" +
"[options] are used for providing runtime information for attaching the agent:\n" +
"\n" +
"    --host <host>                   Hostname or IP address to which to bind on\n" +
"                                    (default: InetAddress.getLocalHost())\n" +
"    --port <port>                   Port to listen on (default: 8778)\n" +
"    --agentContext <context>        HTTP Context under which the agent is reachable (default: " + ConfigKey.AGENT_CONTEXT.getDefaultValue() + ")\n" +
"    --agentId <agent-id>            VM unique identifier used by this agent (default: autogenerated)\n" +
"    --agentDescription <desc>       Agent description\n" +
"    --authMode <mode>               Authentication mode: 'basic' (default), 'jaas' or 'delegate'\n" +
"    --authMatch <match>             If MultiAuthenticator is used, this config item explains how to combine multiple authenticators\n" +
"                                     \"any\" -- at least one authenticator must match (default)\n" +
"                                     \"all\" -- all authenticators must match\n" +
"    --authClass <class>             Classname of an custom Authenticator which must be loadable from the classpath\n" +
"    --authUrl <url>                 URL used for a dispatcher authentication (authMode == delegate)\n" +
"    --authPrincipalSpec <spec>      Extractor specification for getting the principal (authMode == delegate)\n" +
"    --authIgnoreCerts               Whether to ignore CERTS when doing a dispatching authentication (authMode == delegate)\n" +
"    --startTimeout                  Timeout waiting for agent to start (default: 500ms)\n" +
"    --user <user>                   User used for Basic-Authentication\n" +
"    --password <password>           Password used for Basic-Authentication\n" +
"    --quiet                         No output. \"status\" will exit with code 0 if the agent is running, 1 otherwise\n" +
"    --verbose                       Verbose output\n" +
"    --executor <executor>           Executor policy for HTTP Threads to use (default: single)\n" +
"                                     \"fixed\"  -- Thread pool with a fixed number of threads (default: 5)\n" +
"                                     \"cached\" -- Cached Thread Pool, creates threads on demand\n" +
"                                     \"single\" -- Single Thread\n" +
"    --threadNamePrefix <prefix>     Thread name prefix that executor will use while creating new thread(s)\n" +
"                                    (default: jolokia-)\n" +
"    --threadNr <nr threads>         Number of fixed threads if \"fixed\" is used as executor\n" +
"    --backlog <backlog>             How many request to keep in the backlog (default: 10)\n" +
"    --protocol <http|https>         Protocol which must be either \"http\" or \"https\" (default: http)\n" +
"    --keystore <keystore>           Path to keystore (https only)\n" +
"    --keystorePassword <pwd>        Password to the keystore (https only)\n" +
"    --useSslClientAuthentication    Use client certificate authentication (https only)\n" +
"    --secureSocketProtocol <name>   Secure protocol (https only, default: TLS)\n" +
"    --keyStoreType <name>           Keystore type (https only, default: JKS)\n" +
"    --keyManagerAlgorithm <name>    Key manager algorithm (https only, default: SunX509)\n" +
"    --trustManagerAlgorithm <name>  Trust manager algorithm (https only, default: SunX509)\n" +
"    --caCert <path>                 Path to a PEM encoded CA cert file (https & sslClientAuth only)\n" +
"    --serverCert <path>             Path to a PEM encoded server cert file (https only)\n" +
"    --serverKey <path>              Path to a PEM encoded server key file (https only)\n"  +
"    --useCertificateReload <s>      Seconds for certificate/ca/key poller interval (https only, defaults to -1 - no reload)\n" +
"    --serverKeyAlgorithm <algo>     Algorithm to use for decrypting the server key (https only, default: RSA)\n" +
"    --clientPrincipal <principal>   Allow only this principal in the client cert (https & sslClientAuth only)\n" +
"                                    If supplied multiple times, any one of the clientPrincipals must match\n" +
"    --extendedClientCheck <t|f>     Additional validation of client certs for the proper key usage (https & sslClientAuth only)\n" +
"    --discoveryEnabled <t|f>        Enable/Disable discovery multicast responses (default: false)\n" +
"    --discoveryAgentUrl <url>       The URL to use for answering discovery requests. Will be autodetected if not given.\n" +
"    --sslProtocol <protocol>        SSL / TLS protocol to enable, can be provided multiple times\n" +
"    --sslCipherSuite <suite>        SSL / TLS cipher suite to enable, can be provided multiple times\n" +
"    --debug                         Switch on agent debugging\n" +
"    --lazy                          Agent will start in lazy fashion. It'll be initialized on first HTTP request\n" +
"    --logHandlerClass <class>       Implementation of org.jolokia.server.core.service.api.LogHandler for logging\n" +
"                                    Available classes: org.jolokia.server.core.service.impl.QuietLogHandler\n" +
"                                                       org.jolokia.server.core.service.impl.JulLogHandler\n" +
"    --logHandlerName <logger name>  A logger name to be used with custom logger implementation\n" +
"    --debugMaxEntries <nr>          Number of debug entries to keep in memory which can be fetched from the Jolokia MBean\n" +
"    --maxDepth <depth>              Maximum number of levels for serialization of beans\n" +
"    --maxCollectionSize <size>      Maximum number of element in collections to keep when serializing the response\n" +
"    --maxObjects <nr>               Maximum number of objects to consider for serialization\n" +
"    --serializeLong <number|string> How to serialize longs values\n" +
"    --restrictorClass <class>       Classname of an custom restrictor which must be loadable from the classpath\n" +
"    --disableDetectors              Whether to run detectors that locate environment-specific MBeanServer instances\n" +
"    --enabledServices               A comma-separated list of fully-qualified class names.\n" +
"                                    If this configuration option is specified and is not empty, only the\n" +
"                                    services from this list will be actually used.\n" +
"    --disabledServices              A comma-separated list of fully-qualified class names.\n" +
"                                    If this configuration option is specified and is not empty, all detected\n" +
"                                    services (from `/META-INF/jolokia/services(-default)`) will be filtered\n" +
"                                    to not include the disabled services.\n" +
"    --policyLocation <url>          Location of a Jolokia policy file\n" +
"    --mbeanQualifier <qualifier>    Qualifier to use when registering Jolokia internal MBeans\n" +
"    --canonicalNaming <t|f>         whether to use canonicalName for ObjectNames in 'list' or 'search' (default: true)\n" +
"    --includeStackTrace <t|f>       whether to include StackTraces for error messages (default: false)\n" +
"    --serializeException <t|f>      whether to add a serialized version of the exception in the Jolokia response (default: false)\n" +
"    --includeRequest <t|f>          whether to include entire request in the response (default: true)\n" +
"    --dateFormat <format>           DateFormat to use for serializing dates/times/calendars/temporals (default: yyyy-MM-dd'T'HH:mm:ssXXX)\n" +
"    --dateFormatTimeZone <tz>       TimeZone to use for formatting dates/times/calendars/temporals (default: local time zone)\n" +
"    --config <configfile>           Path to a property file from where to read the configuration\n" +
"    --help                          This help documentation\n" +
"    --version                       Version of this agent (it's " + Version.getAgentVersion() + " btw :)\n" +
"\n" +
"<pid/regexp> can be either a numeric process id or a regular expression. A regular expression is matched\n" +
"against the processes' names (ignoring case) and must be specific enough to select exactly one process.\n" +
"\n" +
"If no <command> is given but only a <pid> the state of the Agent will be toggled\n" +
"between \"start\" and \"stop\"\n" +
"\n" +
"If neither <command> nor <pid> is given, a list of Java processes along with their IDs\n" +
"is printed\n" +
"\n" +
"There are several possible reasons, why attaching to a process can fail:\n" +
"   * The UID of this launcher must be the very *same* as the process to attach to. It's not sufficient to be root.\n" +
"   * The JVM must have HotSpot enabled and be a JVM 1.6 or later.\n" +
"   * It must be a Java process ;-)\n" +
"\n" +
"For more documentation please visit www.jolokia.org"
                          );
    }
}
