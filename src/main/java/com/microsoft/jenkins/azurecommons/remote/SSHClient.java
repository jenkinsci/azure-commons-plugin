/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.remote;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.microsoft.jenkins.azurecommons.Constants;
import hudson.util.Secret;
import org.jenkinsci.plugins.azurecommons.Messages;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * An SSH client used to interact with a remote SSH server.
 */
public class SSHClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final String username;
    private final StandardUsernameCredentials credentials;

    private final JSch jsch;
    private final Session session;

    private PrintStream logger;

    /**
     * SSH client to the remote host with given credentials.
     * <p>
     * The credentials can be one of the two:
     * <ul>
     * <li>{@link SSHUserPrivateKey} with username and SSH private key.</li>
     * <li>Implementation of {@link StandardUsernamePasswordCredentials} with username and password.</li>
     * </ul>
     *
     * @param host        the SSH server name or IP address.
     * @param port        the SSH service port.
     * @param credentials the SSH authentication credentials.
     */
    public SSHClient(
            final String host,
            final int port,
            final StandardUsernameCredentials credentials) {
        this.host = host;
        this.port = port;
        this.username = credentials.getUsername();

        this.jsch = new JSch();
        this.credentials = credentials;
        if (credentials instanceof SSHUserPrivateKey) {
            SSHUserPrivateKey userPrivateKey = (SSHUserPrivateKey) credentials;
            final Secret userPassphrase = userPrivateKey.getPassphrase();
            String passphrase = null;
            if (userPassphrase != null) {
                passphrase = userPassphrase.getPlainText();
            }
            byte[] passphraseBytes = null;

            if (passphrase != null) {
                passphraseBytes = passphrase.getBytes(Constants.DEFAULT_CHARSET);
            }
            int seq = 0;
            for (String privateKey : userPrivateKey.getPrivateKeys()) {
                String name = this.username;
                if (seq++ != 0) {
                    name += "-" + seq;
                }
                try {
                    jsch.addIdentity(name, privateKey.getBytes(Constants.DEFAULT_CHARSET), null, passphraseBytes);
                } catch (JSchException e) {
                    throw new IllegalArgumentException(Messages.SSHClient_failedToCreateSession(), e);
                }
            }
        }

        try {
            this.session = jsch.getSession(this.username, this.host, this.port);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            this.session.setConfig(config);
            if (credentials instanceof StandardUsernamePasswordCredentials) {
                this.session.setPassword(
                        ((StandardUsernamePasswordCredentials) credentials).getPassword().getPlainText());
            }
            this.session.connect();
        } catch (JSchException e) {
            throw new IllegalArgumentException(Messages.SSHClient_failedToCreateSession(), e);
        }
    }

    /**
     * Set the optional logger stream to print the status messages.
     *
     * @param log the logger stream
     * @return the current SSH client with the logger stream updated.
     */
    public SSHClient withLogger(PrintStream log) {
        this.logger = log;
        return this;
    }

    /**
     * Copy local file to the remote path.
     *
     * @param sourceFile the local file.
     * @param remotePath the target remote path, can be either absolute or relative to the user home.
     * @throws JSchException if the underlying SSH session fails.
     */
    public void copyTo(final File sourceFile, final String remotePath) throws JSchException {
        log(Messages.SSHClient_copyFileTo(sourceFile, host, remotePath));
        withChannelSftp(new ChannelSftpConsumer() {
            @Override
            public void apply(final ChannelSftp channel) throws JSchException, SftpException {
                channel.put(sourceFile.getAbsolutePath(), remotePath);
            }
        });
    }

    /**
     * Copy the contents from the {@code InputStream} to the remote path.
     *
     * @param in         the {@code InputStream} containing source contents.
     * @param remotePath the target remote path, can be either absolute or relative to the user home.
     * @throws JSchException if the underlying SSH session fails.
     */
    public void copyTo(final InputStream in, final String remotePath) throws JSchException {
        try {
            withChannelSftp(new ChannelSftpConsumer() {
                @Override
                public void apply(final ChannelSftp channel) throws JSchException, SftpException {
                    channel.put(in, remotePath);
                }
            });
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log("Failed to close input stream: " + e.getMessage());
            }
        }
    }

    /**
     * Copy remote file to the local destination.
     *
     * @param remotePath the remote file path, can be either absolute or relative to the user home.
     * @param destFile   the local destination file path.
     * @throws JSchException if the underlying SSH session fails.
     */
    public void copyFrom(final String remotePath, final File destFile) throws JSchException {
        log(Messages.SSHClient_copyFileFrom(host, remotePath, destFile));
        withChannelSftp(new ChannelSftpConsumer() {
            @Override
            public void apply(final ChannelSftp channel) throws JSchException, SftpException {
                channel.get(remotePath, destFile.getAbsolutePath());
            }
        });
    }

    /**
     * Copy remote file contents to the {@code OutputStream}.
     *
     * @param remotePath the remote file path, can be either absolute or relative to the user home.
     * @param out        the {@code OutputStream} where the file contents should be written to.
     * @throws JSchException if the underlying SSH session fails.
     */
    public void copyFrom(final String remotePath, final OutputStream out) throws JSchException {
        withChannelSftp(new ChannelSftpConsumer() {
            @Override
            public void apply(final ChannelSftp channel) throws JSchException, SftpException {
                channel.get(remotePath, out);
            }
        });
    }

    protected void withChannelSftp(final ChannelSftpConsumer consumer) throws JSchException {
        ChannelSftp channel = null;
        try {
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            try {
                consumer.apply(channel);
            } catch (SftpException e) {
                throw new JSchException(Messages.SSHClient_sftpError(), e);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * Execute a command on the remote server and return the command standard output.
     *
     * @param command the command to be executed.
     * @return the standard output of the command.
     * @throws JSchException if the underlying SSH session fails.
     * @throws IOException   if it fails to read the output from the remote channel.
     */
    public String execRemote(final String command) throws JSchException, IOException {
        return execRemote(command, true);
    }

    public String execRemote(final String command, final boolean showCommand) throws JSchException, IOException {
        ChannelExec channel = null;
        try {

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            if (showCommand) {
                log(Messages.SSHClient_execCommand(command));
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[Constants.READ_BUFFER_SIZE];

            channel.connect();
            InputStream in = channel.getInputStream();

            if (logger != null) {
                channel.setErrStream(logger, true);
            }

            while (true) {
                do {
                    // blocks on IO
                    int len = in.read(buffer, 0, buffer.length);
                    if (len < 0) {
                        break;
                    }
                    output.write(buffer, 0, len);
                } while (in.available() > 0);

                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    log(Messages.SSHClient_commandExitStatus(channel.getExitStatus()));
                    if (channel.getExitStatus() != 0) {
                        throw new RuntimeException(Messages.SSHClient_errorExecution(channel.getExitStatus()));
                    }
                    break;
                }
            }
            String serverOutput = output.toString(Constants.DEFAULT_CHARSET.name());
            log(Messages.SSHClient_output(serverOutput));
            return serverOutput;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(Messages.SSHClient_failedExecution(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * Forward another remote SSH port to local through the current client, and create a new client based on the local
     * port.
     * <p>
     * This method assumes that the SSH server on A and B accepts the same authentication credentials.
     *
     * @param remoteHost the target host name or IP address, which is accessible from the SSH target of the current
     *                   SSHClient.
     * @param remotePort the SSH service port on the target host.
     * @return A new SSH client to the target host through the current SSH client.
     * @throws JSchException if error occurs during the SSH operations.
     */
    public SSHClient forwardSSH(final String remoteHost, final int remotePort) throws JSchException {
        return forwardSSH(remoteHost, remotePort, credentials);
    }

    /**
     * Forward another remote SSH port to local through the current client, and create a new client based on the local
     * port.
     * <p>
     * Consider in the case with 2 or more remote severs, where:
     * <ul>
     * <li>We can connect to host A via SSH</li>
     * <li>We want to connect to host B but B is not publicly accessible.</li>
     * <li>A and B are in the same subnet so A can connect to B via SSH.</li>
     * </ul>
     * <p>
     * We can first establish an SSH connection to host A, and then use the port forwarding to forward the connection
     * to the local port through the SSH connection of host A to reach the SSH server on host B.
     * <p>
     * <pre><code>
     *     SSHClient connectionToA = new SSHClient(host_A, port_A, credentials_A);
     *     SSHClient tunnelConnectionToB = connectionToA.forwardSSH(host_B, port_B, credentials_B);
     *     tunnelConnectionToB.execRemote("ls"); // ls executed on host B
     * </code></pre>
     *
     * @param remoteHost the target host name or IP address, which is accessible from the SSH target of the current
     *                   SSHClient.
     * @param remotePort the SSH service port on the target host.
     * @return A new SSH client to the target host through the current SSH client.
     * @throws JSchException if error occurs during the SSH operations.
     */
    public SSHClient forwardSSH(final String remoteHost, final int remotePort,
                                final StandardUsernameCredentials sshCredentials) throws JSchException {
        int localPort = session.setPortForwardingL(0, remoteHost, remotePort);
        return new SSHClient("127.0.0.1", localPort, sshCredentials).withLogger(logger);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public StandardUsernameCredentials getCredentials() {
        return credentials;
    }

    @Override
    public void close() {
        if (this.session != null) {
            this.session.disconnect();
        }
    }

    private void log(final String message) {
        if (logger != null) {
            logger.println(message);
        }
    }

    private interface ChannelSftpConsumer {
        void apply(ChannelSftp channel) throws JSchException, SftpException;
    }
}
