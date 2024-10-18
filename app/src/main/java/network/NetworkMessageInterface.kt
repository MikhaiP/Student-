package network

import models.ContentModel

interface NetworkMessageInterface {
    fun onContent(content: ContentModel)
}