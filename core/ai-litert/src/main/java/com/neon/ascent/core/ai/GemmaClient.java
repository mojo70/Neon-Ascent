package com.neon.ascent.core.ai;

import android.content.Context;
import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.SamplerConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.Role;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Content;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GemmaClient {
    private final Context context;
    private Engine engine = null;
    private boolean isInitializing = false;
    private final String modelPath;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GemmaClient(Context context) {
        this.context = context;
        this.modelPath = new File(context.getExternalFilesDir(null), "gemma.litertlm").getAbsolutePath();
    }

    public CompletableFuture<Void> initialize() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.execute(() -> {
            if (engine != null || isInitializing) {
                future.complete(null);
                return;
            }

            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                future.complete(null);
                return;
            }

            isInitializing = true;
            try {
                // (String modelPath, Backend backend, Backend visionBackend, Backend audioBackend, Integer maxNumTokens, String cacheDir)
                Backend gpuBackend = new Backend.GPU();
                EngineConfig engineConfig = new EngineConfig(
                    modelPath,
                    gpuBackend,
                    gpuBackend, // vision
                    gpuBackend, // audio
                    null, // maxNumTokens
                    context.getCacheDir().getPath()
                );

                Engine newEngine = new Engine(engineConfig);
                newEngine.initialize();
                engine = newEngine;
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            } finally {
                isInitializing = false;
            }
        });
        return future;
    }

    public CompletableFuture<String> generateContent(String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                if (engine == null) {
                    initialize().get();
                }

                if (engine == null) {
                    future.complete("ERROR: GEMMA_ENGINE_OFFLINE");
                    return;
                }

                // (int topK, double topP, double temperature, int seed)
                SamplerConfig samplerConfig = new SamplerConfig(40, 0.9, 1.0, 42);

                // (Contents systemInstruction, List<Message> initialMessages, List<ToolProvider> tools, SamplerConfig samplerConfig, boolean automaticToolCalling, List<Channel> channels)
                ConversationConfig conversationConfig = new ConversationConfig(
                    null, // systemInstruction
                    Collections.emptyList(), // initialMessages
                    Collections.emptyList(), // tools
                    samplerConfig,
                    false, // automaticToolCalling
                    Collections.emptyList() // channels
                );

                Conversation conversation = engine.createConversation(conversationConfig);
                
                Message response = conversation.sendMessage(prompt, Collections.emptyMap());
                List<Content> contents = response.getContents().getContents();
                
                if (contents != null && !contents.isEmpty()) {
                    Content firstContent = contents.get(0);
                    if (firstContent instanceof Content.Text) {
                        future.complete(((Content.Text) firstContent).getText());
                    } else {
                        future.complete(firstContent.toString());
                    }
                } else {
                    future.complete("");
                }
            } catch (Exception e) {
                e.printStackTrace();
                future.complete("ERROR: GEMMA_MALFUNCTION: " + e.getMessage());
            }
        });
        return future;
    }

    public void close() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
        executor.shutdown();
    }

    public boolean isAvailable() {
        return new File(modelPath).exists();
    }
}
