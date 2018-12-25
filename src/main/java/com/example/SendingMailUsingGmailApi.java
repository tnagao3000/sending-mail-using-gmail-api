package com.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

public class SendingMailUsingGmailApi {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";                         // OAuthクライアントID
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);    // OAuthで要求する機能
    private static final String TOKENS_DIRECTORY_PATH = "tokens";                                    // アクセストークンの保存場所

    public static void main(String... args) {

        try {
            final NetHttpTransport TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            // (b-1) プロジェクトのOAuth2.0クライアントID読込
            InputStream in = SendingMailUsingGmailApi.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // (b-2) 送信元メアド所有者の承認を得てアクセストークン取得
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(TRANSPORT, JSON_FACTORY,
                    clientSecrets, SCOPES)
                            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                            .setAccessType("offline").setApprovalPrompt("force").build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            Gmail service = new Gmail.Builder(TRANSPORT, JSON_FACTORY, credential).setApplicationName("demo").build();

            // (b-3) メール作成
            String to = "[ここに送信先メールアドレス]"; // FIXME
            MimeMessage mimeMessage = createEmail(to, null, "送ってみるテスト", "こいつ…動くぞ!");

            // (b-4) メール送信
            sendMessage(service, "me", mimeMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * メール作成と送信
     *
     * これ以降はほぼ次の公式サンプルのまま
     * https://developers.google.com/gmail/api/guides/sending
     */
    public static MimeMessage createEmail(String to, String from, String subject, String bodyText)
            throws MessagingException {

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

//        InternetAddress fromAddress = new InternetAddress(from);
//        email.setFrom(fromAddress);
//        email.setReplyTo(new InternetAddress[] { fromAddress });

        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public static Message sendMessage(Gmail service, String userId, MimeMessage emailContent)
            throws MessagingException, IOException {

        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }
}
