"""Agent 服务配置 — 所有敏感值从环境变量读取，开发环境可用 .env 文件"""

import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # ===================== DeepSeek AI =====================
    DEEPSEEK_API_KEY: str = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_API_URL: str = os.getenv("DEEPSEEK_API_URL", "https://api.deepseek.com/v1")
    DEEPSEEK_MODEL: str = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

    # ===================== Qdrant 向量数据库 =====================
    QDRANT_HOST: str = os.getenv("QDRANT_HOST", "localhost")
    QDRANT_PORT: int = int(os.getenv("QDRANT_PORT", "6333"))
    QDRANT_API_KEY: str = os.getenv("QDRANT_API_KEY", "dyqdrant2024")
    QDRANT_COLLECTION: str = os.getenv("QDRANT_COLLECTION", "video_embeddings")

    # ===================== 通义文本向量模型 (语义检索) =====================
    EMBEDDING_API_KEY: str = os.getenv("EMBEDDING_API_KEY", "")
    EMBEDDING_API_URL: str = os.getenv("EMBEDDING_API_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings")
    EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "text-embedding-v3")
    EMBEDDING_DIM: int = int(os.getenv("EMBEDDING_DIM", "1024"))

    # ===================== Spring Boot 业务后端 =====================
    SPRING_BOOT_API_URL: str = os.getenv("SPRING_BOOT_API_URL", "http://localhost:8080")

    # ===================== 服务端口 =====================
    AGENT_PORT: int = int(os.getenv("AGENT_PORT", "8000"))


settings = Settings()
