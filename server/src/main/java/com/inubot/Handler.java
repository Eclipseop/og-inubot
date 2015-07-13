package com.inubot;

import com.inubot.model.Account;
import com.inubot.model.Owned;
import com.inubot.model.Script;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author Septron
 * @since July 07, 2015
 */
public class Handler extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    private static String getMD5(String message)
            throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(message.getBytes());
        byte[] digest = md5.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        ByteBuf buffer = (ByteBuf) msg;

        int opcode = buffer.readByte();
        switch (opcode) {
            case 0: {
                StringBuilder builder = new StringBuilder();
                int ulength = buffer.readInt();
                for (int i = 0; i < ulength; i++) {
                    builder.append(buffer.readChar());
                }
                String username = builder.toString();

                builder = new StringBuilder();
                int plength = buffer.readInt();
                for (int i = 0; i < plength; i++) {
                    builder.append(buffer.readChar());
                }
                String password = builder.toString();

                Session session = Application.factory().openSession();
                Account account = (Account) session.createQuery("from Account where username=:username")
                        .setParameter("username", username).uniqueResult();
                if (account == null) {
                    logger.info("Account doesn't exist!");
                    return;
                }

                String hash = getMD5(getMD5(account.getSalt()) + getMD5(password));

                if (hash.equals(account.getPassword())) {
                    logger.info("Successfully logged " + username + " into account!");
                    List owneds = session.createQuery("from Owned where uid=:uid")
                            .setParameter("uid", account.getId()).list();
                    for (Object asd : owneds) {
                        Script script = (Script) session.createQuery("from Script where id=:id")
                                .setParameter("id", ((Owned) asd).getId()).uniqueResult();
                        byte[] data = Loader.scripts.get(script.getName());
                        ctx.write(data);
                        logger.info("Sent: "  + script.getName());
                    }
                } else {
                    logger.info("Failed to log " + username + " into account!");
                }
                break;
            }
        }
    }
}
