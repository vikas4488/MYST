package com.vikas.myst.util;

import android.content.Context;
import android.os.AsyncTask;

import com.vikas.myst.bean.Chat;

import java.io.File;
import java.util.List;

public class ParallelProcess {




    public static class DeleteChatsParams {
            List<Chat> chats;
            Context context;

            public DeleteChatsParams(List<Chat> chats, Context ctx) {
                this.chats = chats;
                this.context = ctx;
            }
        }
        public static final class DeleteChatFromDbInBackground extends AsyncTask<DeleteChatsParams, DeleteChatsParams, DeleteChatsParams[]> {
            @Override
            protected DeleteChatsParams[] doInBackground(DeleteChatsParams... chatsParams) {
                return chatsParams;
            }

            @Override
            protected void onPostExecute(final DeleteChatsParams[] chatsParams) {
                for (Chat chat : chatsParams[0].chats) {
                    final String exPath = chat.getMsg();
                    File file=new File(exPath);
                    //System.out.println("----------------------> "+chat.getStatus());
                    if(file.exists())
                        file.delete();
                }
            }
        }

}
